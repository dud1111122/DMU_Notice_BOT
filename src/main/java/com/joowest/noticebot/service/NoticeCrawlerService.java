package com.joowest.noticebot.service;

import com.joowest.noticebot.domain.Notice;
import com.joowest.noticebot.repository.NoticeRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class NoticeCrawlerService {

    private final NoticeRepository noticeRepository;
    private final GeminiService geminiService;
    private final DiscordBotService discordBotService;

    public NoticeCrawlerService(NoticeRepository noticeRepository,
                                GeminiService geminiService,
                                DiscordBotService discordBotService) {
        this.noticeRepository = noticeRepository;
        this.geminiService = geminiService;
        this.discordBotService = discordBotService;
    }

    public void checkLatestNotice() throws Exception {

        String listUrl =
                "https://www.dongyang.ac.kr/dmu/4580/subview.do?enc=Zm5jdDF8QEB8JTJGY29tYkJicyUyRmRtdSUyRjg0JTJGbGlzdC5kbyUzRg%3D%3D";

        Document doc = Jsoup.connect(listUrl)
                .userAgent("Mozilla/5.0")
                .get();

        Element firstRow = doc.selectFirst("tbody tr");
        if (firstRow == null) return;

        Element link = firstRow.selectFirst("td.td-subject a");
        if (link == null) return;

        String title = link.text().trim();
        String date = firstRow.selectFirst("td.td-date").text();
        String href = link.attr("href");

        String[] numbers = href.replaceAll("[^0-9]", " ")
                .trim()
                .split("\\s+");

        if (numbers.length == 0) return;

        String postId = numbers[numbers.length - 1];

        // 🔥 중복 검사
        if (noticeRepository.existsById(postId)) {
            System.out.println("이미 저장된 공지");
            return;
        }

        String detailUrl =
                "https://www.dongyang.ac.kr/combBbs/dmu/84/294/"
                        + postId + "/view.do";

        Document detailDoc = Jsoup.connect(detailUrl)
                .userAgent("Mozilla/5.0")
                .get();

        Element contentDiv = detailDoc.selectFirst("div.view-con");
        String bodyText = contentDiv != null ? contentDiv.text() : "";

        String summary = geminiService.summarize(bodyText);
        LocalDate deadline = geminiService.extractDeadline(bodyText);

        String message =
                "📢 **[새 공지]**\n\n" +
                        "📌 제목: " + title + "\n" +
                        "📅 작성일: " + date + "\n" +
                        (deadline != null ? "⏰ 마감일: " + deadline + "\n" : "") +
                        "\n📝 요약:\n" +
                        summary + "\n\n" +
                        "🔗 " + detailUrl;

        discordBotService.sendNoticeToAllConfiguredChannels(message);

        Notice notice = new Notice(
                postId,
                title,
                date,
                detailUrl,
                summary,
                deadline
        );

        noticeRepository.save(notice);

        System.out.println("공지 저장 및 디스코드 전송 완료");
    }
}
