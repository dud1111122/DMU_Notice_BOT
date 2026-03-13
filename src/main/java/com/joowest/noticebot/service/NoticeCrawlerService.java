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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NoticeCrawlerService {

    private static final String DMU_BASE_URL = "https://www.dongyang.ac.kr";

    private final NoticeRepository noticeRepository;
    private final DepartmentRepository departmentRepository;
    private final GlobalNoticeSourceRepository globalNoticeSourceRepository;
    private final GeminiService geminiService;
    private final DiscordBotService discordBotService;
    private final int requestTimeoutMs;
    private int crawlCursor = 0;

    public NoticeCrawlerService(NoticeRepository noticeRepository,
                                DepartmentRepository departmentRepository,
                                GlobalNoticeSourceRepository globalNoticeSourceRepository,
                                GeminiService geminiService,
                                DiscordBotService discordBotService,
                                @Value("${notice.crawler.request-timeout-ms:7000}") int requestTimeoutMs) {
        this.noticeRepository = noticeRepository;
        this.departmentRepository = departmentRepository;
        this.globalNoticeSourceRepository = globalNoticeSourceRepository;
        this.geminiService = geminiService;
        this.discordBotService = discordBotService;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public void checkLatestNotice() throws Exception {
        for (GlobalNoticeSource source : getActiveGlobalSources()) {
            crawlGlobalNoticeSource(source);
        }

        for (Department department : getActiveDepartments()) {
            crawlDepartmentNotice(department);
        }
    }

    public synchronized void checkNextNotice() throws Exception {
        List<GlobalNoticeSource> globalSources = getActiveGlobalSources();
        List<Department> departments = getActiveDepartments();

        int totalTargets = globalSources.size() + departments.size();
        if (totalTargets == 0) {
            return;
        }

        int currentIndex = Math.floorMod(crawlCursor, totalTargets);
        crawlCursor = (currentIndex + 1) % totalTargets;

        if (currentIndex < globalSources.size()) {
            crawlGlobalNoticeSource(globalSources.get(currentIndex));
            return;
        }

        int departmentIndex = currentIndex - globalSources.size();
        crawlDepartmentNotice(departments.get(departmentIndex));
    }

    private void crawlGlobalNoticeSource(GlobalNoticeSource source) throws Exception {
        NoticeSummary summary = loadLatestNoticeSummary(source.getNoticeUrl());
        if (summary == null) {
            return;
        }

        if (Objects.equals(source.getLastSeenExternalId(), summary.externalId())) {
            return;
        }

        if (noticeRepository.existsByGlobalNoticeSourceIdAndExternalId(source.getId(), summary.externalId())) {
            updateLastSeenExternalId(source, summary.externalId());
            return;
        }

        NoticeDetail detail = loadNoticeDetail(summary);
        if (detail == null) {
            return;
        }

        String noticeSummary = detail.bodyText().isBlank() ? "" : geminiService.summarize(detail.bodyText());
        Notice savedNotice = noticeRepository.save(Notice.builder()
                .globalNoticeSource(source)
                .noticeType(NoticeType.GLOBAL)
                .externalId(summary.externalId())
                .title(summary.title())
                .url(summary.detailUrl())
                .summary(noticeSummary)
                .postedAt(parsePostedAt(summary.rawDate()))
                .build());

        updateLastSeenExternalId(source, summary.externalId());

        String message = buildGlobalMessage(source, summary.title(), summary.rawDate(), summary.detailUrl(), noticeSummary);
        discordBotService.sendNoticeToConfiguredGuilds(savedNotice, detail.bodyText(), message);

        System.out.println("학교 전체공지 저장 및 채널 전송 완료: " + source.getSourceCode() + " / " + summary.title());
    }

    private void crawlDepartmentNotice(Department department) throws Exception {
        NoticeSummary summary = loadLatestNoticeSummary(department.getNoticeUrl());
        if (summary == null) {
            return;
        }

        if (Objects.equals(department.getLastSeenExternalId(), summary.externalId())) {
            return;
        }

        if (noticeRepository.existsByDepartmentIdAndExternalId(department.getId(), summary.externalId())) {
            updateLastSeenExternalId(department, summary.externalId());
            return;
        }

        NoticeDetail detail = loadNoticeDetail(summary);
        if (detail == null) {
            return;
        }

        String noticeSummary = detail.bodyText().isBlank() ? "" : geminiService.summarize(detail.bodyText());
        Notice savedNotice = noticeRepository.save(Notice.builder()
                .department(department)
                .noticeType(NoticeType.DEPARTMENT)
                .externalId(summary.externalId())
                .title(summary.title())
                .url(summary.detailUrl())
                .summary(noticeSummary)
                .postedAt(parsePostedAt(summary.rawDate()))
                .build());

        updateLastSeenExternalId(department, summary.externalId());

        String message = buildDepartmentMessage(department, summary.title(), summary.rawDate(), summary.detailUrl(), noticeSummary);
        discordBotService.sendNoticeToConfiguredGuilds(savedNotice, detail.bodyText(), message);

        System.out.println("학과 공지 저장 및 채널 전송 완료: " + department.getDeptCode() + " / " + summary.title());
    }

    private NoticeSummary loadLatestNoticeSummary(String noticeUrl) throws Exception {
        Document listDoc = Jsoup.connect(noticeUrl)
                .userAgent("Mozilla/5.0")
                .timeout(requestTimeoutMs)
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

        return new NoticeSummary(title, rawDate, detailUrl, extractExternalId(detailUrl));
    }

    private NoticeDetail loadNoticeDetail(NoticeSummary summary) throws Exception {
        Document detailDoc = Jsoup.connect(summary.detailUrl())
                .userAgent("Mozilla/5.0")
                .timeout(requestTimeoutMs)
                .get();

        Element contentDiv = detailDoc.selectFirst("div.view-con, div#bbs-content, div.board-view-content");
        String bodyText = contentDiv != null ? contentDiv.text() : "";
        return new NoticeDetail(bodyText);
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

    private void updateLastSeenExternalId(GlobalNoticeSource source, String externalId) {
        if (Objects.equals(source.getLastSeenExternalId(), externalId)) {
            return;
        }
        source.setLastSeenExternalId(externalId);
        globalNoticeSourceRepository.save(source);
    }

    private void updateLastSeenExternalId(Department department, String externalId) {
        if (Objects.equals(department.getLastSeenExternalId(), externalId)) {
            return;
        }
        department.setLastSeenExternalId(externalId);
        departmentRepository.save(department);
    }

    private List<GlobalNoticeSource> getActiveGlobalSources() {
        return globalNoticeSourceRepository.findByEnabledTrueOrderBySortOrderAscSourceNameAsc().stream()
                .filter(source -> source.getNoticeUrl() != null && !source.getNoticeUrl().isBlank())
                .toList();
    }

    private List<Department> getActiveDepartments() {
        return departmentRepository.findByEnabledTrueOrderBySortOrderAscDeptNameAsc().stream()
                .filter(department -> department.getNoticeUrl() != null && !department.getNoticeUrl().isBlank())
                .toList();
    }

    private record NoticeSummary(String title, String rawDate, String detailUrl, String externalId) {
    }

    private record NoticeDetail(String bodyText) {
    }
}
