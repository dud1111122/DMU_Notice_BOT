package com.joowest.noticebot.service;

import com.joowest.noticebot.domain.Department;
import com.joowest.noticebot.domain.Notice;
import com.joowest.noticebot.repository.DepartmentRepository;
import com.joowest.noticebot.repository.NoticeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class NoticeCrawlerService {

    private static final String DMU_BASE_URL = "https://www.dongyang.ac.kr";

    private final NoticeRepository noticeRepository;
    private final DepartmentRepository departmentRepository;
    private final GeminiService geminiService;
    private final DiscordBotService discordBotService;

    public NoticeCrawlerService(NoticeRepository noticeRepository,
                                DepartmentRepository departmentRepository,
                                GeminiService geminiService,
                                DiscordBotService discordBotService) {
        this.noticeRepository = noticeRepository;
        this.departmentRepository = departmentRepository;
        this.geminiService = geminiService;
        this.discordBotService = discordBotService;
    }

    public void checkLatestNotice() throws Exception {
        List<Department> departments = departmentRepository.findByEnabledTrueOrderBySortOrderAscDeptNameAsc();
        for (Department department : departments) {
            if (department.getNoticeUrl() == null || department.getNoticeUrl().isBlank()) {
                continue;
            }

            crawlLatestNoticeForDepartment(department);
        }
    }

    private void crawlLatestNoticeForDepartment(Department department) throws Exception {
        Document listDoc = Jsoup.connect(department.getNoticeUrl())
                .userAgent("Mozilla/5.0")
                .get();

        Element firstRow = listDoc.selectFirst("tbody tr");
        if (firstRow == null) {
            return;
        }

        Element link = firstRow.selectFirst("td.td-subject a, td a");
        if (link == null) {
            return;
        }

        String title = link.text().trim();
        String rawDate = firstRow.selectFirst("td.td-date") != null
                ? firstRow.selectFirst("td.td-date").text().trim()
                : "";

        String detailUrl = resolveDetailUrl(link);
        if (detailUrl == null || detailUrl.isBlank()) {
            return;
        }

        String externalId = extractExternalId(detailUrl);
        if (noticeRepository.existsByDepartmentIdAndExternalId(department.getId(), externalId)) {
            return;
        }

        Document detailDoc = Jsoup.connect(detailUrl)
                .userAgent("Mozilla/5.0")
                .get();

        Element contentDiv = detailDoc.selectFirst("div.view-con, div#bbs-content, div.board-view-content");
        String bodyText = contentDiv != null ? contentDiv.text() : "";
        String summary = bodyText.isBlank() ? "" : geminiService.summarize(bodyText);

        Notice savedNotice = noticeRepository.save(Notice.builder()
                .department(department)
                .externalId(externalId)
                .title(title)
                .url(detailUrl)
                .summary(summary)
                .postedAt(parsePostedAt(rawDate))
                .build());

        String message = buildMessage(department, title, rawDate, detailUrl, summary);
        discordBotService.sendNoticeToConfiguredGuilds(savedNotice, bodyText, message);

        System.out.println("공지 저장 및 채널 전송 완료: " + department.getDeptCode() + " / " + title);
    }

    private String buildMessage(Department department, String title, String rawDate, String detailUrl, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("📢 **[새 공지]**\n\n");
        sb.append("🏫 학과: ").append(department.getDeptName()).append("\n");
        sb.append("📌 제목: ").append(title).append("\n");
        if (rawDate != null && !rawDate.isBlank()) {
            sb.append("📅 작성일: ").append(rawDate).append("\n");
        }
        if (summary != null && !summary.isBlank() && !"ERROR".equals(summary)) {
            sb.append("\n📝 요약:\n").append(summary).append("\n");
        }
        sb.append("\n🔗 ").append(detailUrl);
        return sb.toString();
    }

    private LocalDateTime parsePostedAt(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> dateTimeFormats = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA),
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.KOREA)
        );

        for (DateTimeFormatter formatter : dateTimeFormats) {
            try {
                return LocalDateTime.parse(rawDate, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        List<DateTimeFormatter> dateFormats = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA),
                DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA),
                DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.KOREA)
        );

        for (DateTimeFormatter formatter : dateFormats) {
            try {
                return LocalDate.parse(rawDate, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    private String resolveDetailUrl(Element link) {
        String abs = link.absUrl("href");
        if (!abs.isBlank() && (abs.startsWith("http://") || abs.startsWith("https://"))) {
            return abs;
        }

        String href = link.attr("href");
        if (href == null || href.isBlank()) {
            return null;
        }
        if (href.startsWith("javascript:jf_combBbs_view")) {
            List<String> args = extractQuotedArgs(href);
            if (args.size() >= 4) {
                return DMU_BASE_URL + "/combBbs/" + args.get(0) + "/" + args.get(1) + "/" + args.get(2) + "/" + args.get(3) + "/view.do";
            }
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (href.startsWith("/")) {
            return DMU_BASE_URL + href;
        }
        return DMU_BASE_URL + "/" + href;
    }

    private List<String> extractQuotedArgs(String javascriptHref) {
        Matcher matcher = Pattern.compile("'([^']*)'").matcher(javascriptHref);
        List<String> args = new ArrayList<>();
        while (matcher.find()) {
            args.add(matcher.group(1));
        }
        return args;
    }

    private String extractExternalId(String detailUrl) {
        String[] numbers = detailUrl.replaceAll("[^0-9]", " ")
                .trim()
                .split("\\s+");
        if (numbers.length == 0) {
            return Integer.toHexString(Objects.hash(detailUrl));
        }
        return numbers[numbers.length - 1];
    }
}
