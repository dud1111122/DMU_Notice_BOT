package com.joowest.noticebot.service;

import com.joowest.noticebot.domain.GuildSetting;
import com.joowest.noticebot.domain.UserSubscription;
import com.joowest.noticebot.repository.GuildSettingRepository;
import com.joowest.noticebot.repository.UserSubscriptionRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DiscordBotService {

    private static final String ALL_DEPT = "__ALL__";

    private final JDA jda;
    private final GuildSettingRepository guildSettingRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    public DiscordBotService(JDA jda,
                             GuildSettingRepository guildSettingRepository,
                             UserSubscriptionRepository userSubscriptionRepository) {
        this.jda = jda;
        this.guildSettingRepository = guildSettingRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
    }

    // 기존 방식 (채널 ID 직접 지정)
    public void sendMessage(String channelId, String message) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            System.out.println("채널을 찾을 수 없습니다: " + channelId);
        }
    }

    // 새로운 방식 (서버별 설정된 채널로 전송)
    public void sendNoticeToGuild(String guildId, String message) {
        GuildSetting setting = guildSettingRepository.findByGuildId(guildId);

        if (setting != null) {
            TextChannel channel = jda.getTextChannelById(setting.getChannelId());
            if (channel != null) {
                channel.sendMessage(message).queue();
            } else {
                System.out.println("설정된 채널을 찾을 수 없습니다: " + setting.getChannelId());
            }
        } else {
            System.out.println("서버 설정을 찾을 수 없습니다: " + guildId);
        }
    }

    public void sendNoticeToAllConfiguredChannels(String message) {
        List<GuildSetting> allSettings = guildSettingRepository.findAll();

        for (GuildSetting setting : allSettings) {
            if (Boolean.FALSE.equals(setting.getEnabled())) {
                continue;
            }
            TextChannel channel = jda.getTextChannelById(setting.getChannelId());
            if (channel != null) {
                channel.sendMessage(message).queue();
            } else {
                System.out.println("설정된 채널을 찾을 수 없습니다: " + setting.getChannelId());
            }
        }
    }

    public void sendNoticeToSubscribersByDept(String deptCode, String message) {
        List<GuildSetting> allSettings = guildSettingRepository.findByEnabledTrue();

        for (GuildSetting setting : allSettings) {
            TextChannel channel = jda.getTextChannelById(setting.getChannelId());
            if (channel == null) {
                System.out.println("설정된 채널을 찾을 수 없습니다: " + setting.getChannelId());
                continue;
            }

            Set<String> targetUserIds = new LinkedHashSet<>();
            List<UserSubscription> deptSubscribers =
                    userSubscriptionRepository.findByGuildIdAndDeptAndEnabledTrue(setting.getGuildId(), deptCode);
            List<UserSubscription> allSubscribers =
                    userSubscriptionRepository.findByGuildIdAndDeptAndEnabledTrue(setting.getGuildId(), ALL_DEPT);

            deptSubscribers.forEach(s -> targetUserIds.add(s.getUserId()));
            allSubscribers.forEach(s -> targetUserIds.add(s.getUserId()));

            if (targetUserIds.isEmpty()) {
                continue;
            }

            String mentions = targetUserIds.stream()
                    .map(id -> "<@" + id + ">")
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");

            channel.sendMessage(mentions + "\n" + message).queue();
        }
    }
}
