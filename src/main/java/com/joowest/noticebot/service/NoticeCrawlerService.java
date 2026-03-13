package com.joowest.noticebot.service;

import com.joowest.noticebot.domain.Department;
import com.joowest.noticebot.domain.GlobalNoticeSource;
import com.joowest.noticebot.domain.Notice;
import com.joowest.noticebot.domain.NoticeType;
import com.joowest.noticebot.repository.DepartmentRepository;
import com.joowest.noticebot.repository.GlobalNoticeSourceRepository;
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
    private final GlobalNoticeSourceRepository globalNoticeSourceRepository;
    private final GeminiService geminiService;
    private final DiscordBotService discordBotService;

    public NoticeCrawlerService(NoticeRepository noticeRepository,
                                DepartmentRepository departmentRepository,
                                GlobalNoticeSourceRepository globalNoticeSourceRepository,
                                GeminiService geminiService,
                                DiscordBotService discordBotService) {
        this.noticeRepository = noticeRepository;
        this.departmentRepository = departmentRepository;
        this.globalNoticeSourceRepository = globalNoticeSourceRepository;
        this.geminiService = geminiService;
        this.discordBotService = discordBotService;
    }

    public void checkLatestNotice() throws Exception {
        for (GlobalNoticeSource source : globalNoticeSourceRepository.findByEnabledTrueOrderBySortOrderAscSourceNameAsc()) {
            if (source.getNoticeUrl() == null || source.getNoticeUrl().isBlank()) {
                continue;
            }
            crawlGlobalNoticeSource(source);
        }

        List<Department> departments = departmentRepository.findByEnabledTrueOrderBySortOrderAscDeptNameAsc();
        for (Department department : departments) {
            if (department.getNoticeUrl() == null || department.getNoticeUrl().isBlank()) {
                continue;
            }
            crawlDepartmentNotice(department);
        }
    }

    private void crawlGlobalNoticeSource(GlobalNoticeSource source) throws Exception {
        NoticeCandidate candidate = loadLatestNoticeCandidate(source.getNoticeUrl());
        if (candidate == null) {
            return;
        }

        String externalId = extractExternalId(candidate.detailUrl());
        if (noticeRepository.existsByGlobalNoticeSourceIdAndExternalId(source.getId(), externalId)) {
            return;
        }

        String summary = candidate.bodyText().isBlank() ? "" : geminiService.summarize(candidate.bodyText());
        Notice savedNotice = noticeRepository.save(Notice.builder()
                .globalNoticeSource(source)
                .noticeType(NoticeType.GLOBAL)
                .externalId(externalId)
                .title(candidate.title())
                .url(candidate.detailUrl())
                .summary(summary)
                .postedAt(parsePostedAt(candidate.rawDate()))
                .build());

        String message = buildGlobalMessage(source, candidate.title(), candidate.rawDate(), candidate.detailUrl(), summary);
        discordBotService.sendNoticeToConfiguredGuilds(savedNotice, candidate.bodyText(), message);

        System.out.println("학교 전체공지 저장 및 채널 전송 완료: " + source.getSourceCode() + " / " + candidate.title());
    }

    private void crawlDepartmentNotice(Department department) throws Exception {
        NoticeCandidate candidate = loadLatestNoticeCandidate(department.getNoticeUrl());
        if (candidate == null) {
            return;
        }

        String externalId = extractExternalId(candidate.detailUrl());
        if (noticeRepository.existsByDepartmentIdAndExternalId(department.getId(), externalId)) {
            return;
        }

        String summary = candidate.bodyText().isBlank() ? "" : geminiService.summarize(candidate.bodyText());
        Notice savedNotice = noticeRepository.save(Notice.builder()
                .department(department)
                .noticeType(NoticeType.DEPARTMENT)
                .externalId(externalId)
                .title(candidate.title())
                .url(candidate.detailUrl())
                .summary(summary)
                .postedAt(parsePostedAt(candidate.rawDate()))
                .build());

        String message = buildDepartmentMessage(department, candidate.title(), candidate.rawDate(), candidate.detailUrl(), summary);
        discordBotService.sendNoticeToConfiguredGuilds(savedNotice, candidate.bodyText(), message);

        System.out.println("학과 공지 저장 및 채널 전송 완료: " + department.getDeptCode() + " / " + candidate.title());
    }

    private NoticeCandidate loadLatestNoticeCandidate(String noticeUrl) throws Exception {
        Document listDoc = Jsoup.connect(noticeUrl)
                .userAgent("Mozilla/5.0")
                .get();

        Element firstRow = listDoc.selectFirst("tbody tr");
        if (firstRow == null) {
            return null;
        }

        Element link = firstRow.selectFirst("td.td-subject a, td a");
        if (link == null) {
            return null;
        }

        String title = extractTitle(link);
        String rawDate = firstRow.selectFirst("td.td-date") != null
                ? firstRow.selectFirst("td.td-date").text().trim()
                : "";

        String detailUrl = resolveDetailUrl(link);
        if (detailUrl == null || detailUrl.isBlank()) {
            return null;
        }

        Document detailDoc = Jsoup.connect(detailUrl)
                .userAgent("Mozilla/5.0")
                .get();

        Element contentDiv = detailDoc.selectFirst("div.view-con, div#bbs-content, div.board-view-content");
        String bodyText = contentDiv != null ? contentDiv.text() : "";
        return new NoticeCandidate(title, rawDate, detailUrl, bodyText);
    }

    private String buildGlobalMessage(GlobalNoticeSource source, String title, String rawDate, String detailUrl, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("📢 **[학교 전체공지]**\n\n");
        sb.append("🏫 분류: ").append(source.getSourceName()).append("\n");
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

    private String buildDepartmentMessage(Department department, String title, String rawDate, String detailUrl, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("📢 **[학과 공지]**\n\n");
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

    private String extractTitle(Element link) {
        Element strong = link.selectFirst("strong");
        if (strong != null && !strong.text().isBlank()) {
            return strong.text().trim();
        }
        return link.text().trim();
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

    private record NoticeCandidate(String title, String rawDate, String detailUrl, String bodyText) {
    }
}
