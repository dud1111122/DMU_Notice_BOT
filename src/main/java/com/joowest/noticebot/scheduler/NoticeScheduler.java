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

    // 🔥 5분마다 실행 (테스트용이면 10000으로 바꿔도 됨)
    @Scheduled(fixedRate = 300000)
    public void run() {
        try {
            System.out.println("공지 체크 중...");
            crawlerService.checkLatestNotice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}