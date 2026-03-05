package com.joowest.noticebot.listener;

import com.joowest.noticebot.domain.GuildSetting;
import com.joowest.noticebot.repository.GuildSettingRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Component;

@Component
public class DiscordListener extends ListenerAdapter {

    private final GuildSettingRepository guildSettingRepository;

    public DiscordListener(GuildSettingRepository guildSettingRepository) {
        this.guildSettingRepository = guildSettingRepository;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("설정")) {
            handleSettingCommand(event);
        } else if (event.getName().equals("테스트")) {
            handleTestCommand(event);
        }
    }

    private void handleSettingCommand(SlashCommandInteractionEvent event) {
        String guildId = event.getGuild().getId();
        String channelId = event.getOption("채널선택").getAsChannel().getId();
        String channelName = event.getOption("채널선택").getAsChannel().getName();

        GuildSetting existingSetting = guildSettingRepository.findByGuildId(guildId);

        if (existingSetting != null) {
            existingSetting.setChannelId(channelId);
            guildSettingRepository.save(existingSetting);
        } else {
            GuildSetting newSetting = GuildSetting.builder()
                    .guildId(guildId)
                    .channelId(channelId)
                    .build();
            guildSettingRepository.save(newSetting);
        }

        // 설정 완료 메시지 (채널 정보 포함)
        String confirmMessage = "✅ **설정이 완료되었습니다!**\n\n" +
                "📍 공지 채널: #" + channelName + "\n" +
                "🤖 앞으로 이 채널로 공지 알림을 받게 됩니다.";

        event.reply(confirmMessage).queue();
    }

    private void handleTestCommand(SlashCommandInteractionEvent event) {
        String guildId = event.getGuild().getId();
        GuildSetting setting = guildSettingRepository.findByGuildId(guildId);

        if (setting == null) {
            event.reply("❌ **설정이 필요합니다!**\n\n" +
                    "먼저 `/설정 채널선택 #채널명` 명령어로 공지 채널을 설정해주세요.").queue();
            return;
        }

        // 테스트 메시지 생성
        String testMessage = "🧪 **테스트 메시지**\n\n" +
                "✅ Discord 봇이 정상적으로 작동하고 있습니다!\n" +
                "✅ MongoDB 연결이 정상입니다.\n" +
                "✅ 공지 채널 설정이 완료되었습니다.\n\n" +
                "📅 테스트 시간: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 설정된 채널로 테스트 메시지 전송
        TextChannel channel = event.getJDA().getTextChannelById(setting.getChannelId());
        if (channel != null) {
            channel.sendMessage(testMessage).queue();
        } else {
            event.reply("❌ **채널을 찾을 수 없습니다!**\n\n" +
                    "설정된 채널이 삭제되었을 수 있습니다. 다시 설정해주세요.").setEphemeral(true).queue();
            return;
        }

        // 사용자에게 확인 메시지
        event.reply("✅ **테스트 메시지를 전송했습니다!**\n\n" +
                "설정된 공지 채널에서 확인해보세요.").queue();
    }

    public void registerSlashCommands(JDA jda) {
        jda.updateCommands()
                .addCommands(
                        Commands.slash("설정", "공지 채널을 설정합니다")
                                .addOption(OptionType.CHANNEL, "채널선택", "공지를 받을 채널을 선택하세요", true),
                        Commands.slash("테스트", "테스트 명령어입니다")
                )
                .queue();
    }
}
