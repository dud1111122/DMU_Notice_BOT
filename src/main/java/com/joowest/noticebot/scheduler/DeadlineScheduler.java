package com.joowest.noticebot.scheduler;

import com.joowest.noticebot.domain.Notice;
import com.joowest.noticebot.repository.NoticeRepository;
import com.joowest.noticebot.service.DiscordService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DeadlineScheduler {

    private final NoticeRepository noticeRepository;
    private final DiscordService discordService;

    public DeadlineScheduler(NoticeRepository noticeRepository,
                             DiscordService discordService) {
        this.noticeRepository = noticeRepository;
        this.discordService = discordService;
    }

    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Seoul")
    public void checkDeadlines() {

        LocalDate today = LocalDate.now();
        LocalDate target = today.plusDays(3);

        List<Notice> notices =
                noticeRepository.findByDeadlineAndReminderSentFalse(target);

        for (Notice notice : notices) {

            String message =
                    "🔥 **[마감 3일 전 알림]**\n\n" +
                            "📌 " + notice.getTitle() + "\n" +
                            "⏰ 마감일: " + notice.getDeadline() + "\n\n" +
                            "🔗 " + notice.getUrl();

            discordService.sendMessage(message);

            // 🔥 재알림 보냄 처리
            notice.setReminderSent(true);
            noticeRepository.save(notice);
        }

        if (!notices.isEmpty()) {
            System.out.println("마감 재알림 전송 완료");
        }
    }
}