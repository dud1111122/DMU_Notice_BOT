package com.joowest.noticebot;

import com.joowest.noticebot.listener.DiscordListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import javax.security.auth.login.LoginException;

@SpringBootApplication
@EnableScheduling
public class NoticebotApplication {

    public static void main(String[] args) {

        // 로컬 개발 환경 프로필 활성화
        System.setProperty("spring.profiles.active", "local");

        SpringApplication.run(NoticebotApplication.class, args);
    }

    @Bean
    public JDA jda(@Value("${discord.token}") String discordToken, DiscordListener discordListener) throws LoginException, InterruptedException {
        JDA jda = JDABuilder.createDefault(discordToken)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(discordListener)
                .build()
                .awaitReady();

        // Slash Command 등록
        discordListener.registerSlashCommands(jda);

        return jda;
    }
}
