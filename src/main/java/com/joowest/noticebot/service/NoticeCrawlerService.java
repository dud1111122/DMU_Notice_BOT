package com.joowest.noticebot.service;

import com.joowest.noticebot.domain.Department;
import com.joowest.noticebot.domain.Notice;
import com.joowest.noticebot.repository.DepartmentRepository;
import com.joowest.noticebot.repository.NoticeRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NoticeCrawlerService {

    private static final String DMU_BASE_URL = "https://www.dongyang.ac.kr";

    private final NoticeRepository noticeRepository;
    private final DepartmentRepository departmentRepository;
    private final GeminiService geminiService;
    private final DeadlineParserService deadlineParserService;
    private final DiscordBotService discordBotService;

    public NoticeCrawlerService(NoticeRepository noticeRepository,
                                DepartmentRepository departmentRepository,
                                GeminiService geminiService,
                                DeadlineParserService deadlineParserService,
                                DiscordBotService discordBotService) {
        this.noticeRepository = noticeRepository;
        this.departmentRepository = departmentRepository;
        this.geminiService = geminiService;
        this.deadlineParserService = deadlineParserService;
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
        String date = firstRow.selectFirst("td.td-date") != null
                ? firstRow.selectFirst("td.td-date").text().trim()
                : "";

        String detailUrl = resolveDetailUrl(link);
        if (detailUrl == null || detailUrl.isBlank()) {
            return;
        }

        String postId = extractPostId(detailUrl);
        String noticeId = department.getDeptCode() + "_" + postId;

        if (noticeRepository.existsById(noticeId)) {
            return;
        }

        Document detailDoc = Jsoup.connect(detailUrl)
                .userAgent("Mozilla/5.0")
                .get();

        Element contentDiv = detailDoc.selectFirst("div.view-con, div#bbs-content, div.board-view-content");
        String bodyText = contentDiv != null ? contentDiv.text() : "";

        String summary = geminiService.summarize(bodyText);
        LocalDate deadline = deadlineParserService.extractDeadline(bodyText);

        String message =
                "📢 **[새 공지]**\n\n" +
                        "🏫 학과: " + department.getDeptName() + "\n" +
                        "📌 제목: " + title + "\n" +
                        (!date.isBlank() ? "📅 작성일: " + date + "\n" : "") +
                        (deadline != null ? "⏰ 마감일: " + deadline + "\n" : "") +
                        "\n📝 요약:\n" +
                        summary + "\n\n" +
                        "🔗 " + detailUrl;

        discordBotService.sendNoticeToSubscribersByDept(department.getDeptCode(), message);

        Notice notice = new Notice(
                noticeId,
                title,
                date,
                detailUrl,
                summary,
                department.getDeptCode(),
                department.getDeptName(),
                deadline
        );
        noticeRepository.save(notice);

        System.out.println("공지 저장 및 디스코드 전송 완료: " + department.getDeptCode() + " / " + title);
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

    private String extractPostId(String detailUrl) {
        String[] numbers = detailUrl.replaceAll("[^0-9]", " ")
                .trim()
                .split("\\s+");
        if (numbers.length == 0) {
            return Integer.toHexString(Objects.hash(detailUrl));
        }
        return numbers[numbers.length - 1];
    }
}
