package com.joowest.noticebot.controller;

import com.joowest.noticebot.service.NoticeCrawlerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final NoticeCrawlerService crawlerService;

    public TestController(NoticeCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/crawl")
    public String crawl() throws Exception {
        crawlerService.checkLatestNotice();
        return "테스트 완료";
    }
}