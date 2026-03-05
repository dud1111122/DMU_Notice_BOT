package com.joowest.noticebot.service;

import com.joowest.noticebot.domain.GuildSetting;
import com.joowest.noticebot.repository.GuildSettingRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscordBotService {

    private final JDA jda;
    private final GuildSettingRepository guildSettingRepository;

    @Autowired
    public DiscordBotService(JDA jda, GuildSettingRepository guildSettingRepository) {
        this.jda = jda;
        this.guildSettingRepository = guildSettingRepository;
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
            TextChannel channel = jda.getTextChannelById(setting.getChannelId());
            if (channel != null) {
                channel.sendMessage(message).queue();
            } else {
                System.out.println("설정된 채널을 찾을 수 없습니다: " + setting.getChannelId());
            }
        }
    }
}
