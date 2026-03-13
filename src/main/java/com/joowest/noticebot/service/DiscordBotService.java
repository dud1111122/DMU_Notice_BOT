package com.joowest.noticebot.service;

import com.joowest.noticebot.domain.GuildSetting;
import com.joowest.noticebot.domain.Keyword;
import com.joowest.noticebot.domain.Notification;
import com.joowest.noticebot.domain.Notice;
import com.joowest.noticebot.domain.NoticeType;
import com.joowest.noticebot.domain.Subscription;
import com.joowest.noticebot.domain.UserSetting;
import com.joowest.noticebot.repository.GuildSettingRepository;
import com.joowest.noticebot.repository.KeywordRepository;
import com.joowest.noticebot.repository.NotificationRepository;
import com.joowest.noticebot.repository.SubscriptionRepository;
import com.joowest.noticebot.repository.UserSettingRepository;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;

@Service
public class DiscordBotService {

    private final JDA jda;
    private final GuildSettingRepository guildSettingRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserSettingRepository userSettingRepository;
    private final KeywordRepository keywordRepository;
    private final NotificationRepository notificationRepository;

    public DiscordBotService(JDA jda,
                             GuildSettingRepository guildSettingRepository,
                             SubscriptionRepository subscriptionRepository,
                             UserSettingRepository userSettingRepository,
                             KeywordRepository keywordRepository,
                             NotificationRepository notificationRepository) {
        this.jda = jda;
        this.guildSettingRepository = guildSettingRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userSettingRepository = userSettingRepository;
        this.keywordRepository = keywordRepository;
        this.notificationRepository = notificationRepository;
    }

    public void sendNoticeToConfiguredGuilds(Notice notice, String bodyText, String message) {
        if (notice.getId() == null) {
            return;
        }

        for (GuildSetting guildSetting : guildSettingRepository.findByEnabledTrue()) {
            if (guildSetting.getChannelId() == null || guildSetting.getChannelId().isBlank()) {
                continue;
            }
            if (notificationRepository.existsByGuildSettingIdAndNoticeId(guildSetting.getId(), notice.getId())) {
                continue;
            }

            TextChannel channel = jda.getTextChannelById(guildSetting.getChannelId());
            if (channel == null) {
                System.out.println("설정된 채널을 찾을 수 없습니다: " + guildSetting.getChannelId());
                continue;
            }

            String payload = buildPayload(guildSetting, notice, bodyText, message);
            if (payload == null) {
                continue;
            }

            channel.sendMessage(payload).queue(
                    success -> notificationRepository.save(Notification.builder()
                            .guildSetting(guildSetting)
                            .notice(notice)
                            .build()),
                    failure -> System.out.println("채널 전송 실패(" + guildSetting.getGuildId() + "): " + failure.getMessage())
            );
        }
    }

    private String buildPayload(GuildSetting guildSetting, Notice notice, String bodyText, String message) {
        Set<String> mentions = new LinkedHashSet<>();
        String searchableText = (notice.getTitle() + "\n" + (bodyText == null ? "" : bodyText))
                .toLowerCase(Locale.ROOT);

        if (notice.getNoticeType() == NoticeType.GLOBAL) {
            for (UserSetting userSetting : userSettingRepository.findByGuildSettingIdAndGlobalNoticeEnabledTrue(guildSetting.getId())) {
                mentions.add("<@" + userSetting.getUser().getDiscordId() + ">");
            }
        } else if (notice.getNoticeType() == NoticeType.DEPARTMENT && notice.getDepartment() != null) {
            for (Subscription subscription : subscriptionRepository.findByGuildSettingIdAndDepartmentIdAndEnabledTrue(
                    guildSetting.getId(),
                    notice.getDepartment().getId()
            )) {
                mentions.add("<@" + subscription.getUser().getDiscordId() + ">");
            }
        }

        for (Keyword keyword : keywordRepository.findByGuildSettingId(guildSetting.getId())) {
            String value = keyword.getKeyword();
            if (value == null || value.isBlank()) {
                continue;
            }
            if (searchableText.contains(value.toLowerCase(Locale.ROOT))) {
                mentions.add("<@" + keyword.getUser().getDiscordId() + ">");
            }
        }

        if (mentions.isEmpty()) {
            return null;
        }

        return String.join(" ", mentions) + "\n" + message;
    }
}
