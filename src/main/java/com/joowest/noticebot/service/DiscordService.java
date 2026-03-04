package com.joowest.noticebot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DiscordService {

    @Value("${discord.webhook-url}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String message) {

        Map<String, String> payload = Map.of("content", message);

        restTemplate.postForObject(
                webhookUrl,
                payload,
                String.class
        );
    }
}