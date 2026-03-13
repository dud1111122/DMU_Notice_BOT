package com.joowest.noticebot.scheduler;

import com.joowest.noticebot.service.NoticeCrawlerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoticeScheduler {

    private final NoticeCrawlerService crawlerService;

    public NoticeScheduler(NoticeCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @Scheduled(
            fixedDelayString = "${notice.crawler.check-interval-ms:12000}",
            initialDelayString = "${notice.crawler.initial-delay-ms:5000}"
    )
    public void run() {
        try {
            crawlerService.checkNextNotice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
