package com.joowest.noticebot.controller;

import com.joowest.noticebot.service.NoticeCrawlerService;
import com.joowest.noticebot.service.DiscordBotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final NoticeCrawlerService crawlerService;
    private final DiscordBotService discordBotService;

    public TestController(
            NoticeCrawlerService crawlerService,
            DiscordBotService discordBotService
    ) {
        this.crawlerService = crawlerService;
        this.discordBotService = discordBotService;
    }

    @GetMapping("/crawl")
    public String crawl() throws Exception {
        crawlerService.checkLatestNotice();
        return "크롤링 테스트 완료";
    }

    @GetMapping("/test-discord")
    public String testDiscord() {

        discordBotService.sendNoticeToAllConfiguredChannels("공지 테스트 메시지입니다");

        return "디스코드 전송 완료";
    }
}
