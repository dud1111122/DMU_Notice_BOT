package com.joowest.noticebot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model.primary:gemini-2.5-flash}")
    private String primaryModel;

    @Value("${gemini.model.fallback:gemini-1.5-flash}")
    private String fallbackModel;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    /**
     * 🔥 공통 Gemini 호출
     */
    private String callGemini(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        List<String> models = buildModelCandidates();

        for (String model : models) {
            String url = String.format(BASE_URL_TEMPLATE, model, apiKey);
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                String parsed = parseGeminiResponse(response);
                if (!"ERROR".equals(parsed)) {
                    return parsed;
                }
            } catch (HttpClientErrorException.TooManyRequests e) {
                System.out.println("Gemini 429 발생, 모델 폴백 시도: " + model);
            } catch (Exception e) {
                System.out.println("Gemini 호출 실패(" + model + "): " + e.getMessage());
            }
        }

        return "ERROR";
    }

    private List<String> buildModelCandidates() {
        List<String> models = new ArrayList<>();

        if (primaryModel != null && !primaryModel.isBlank()) {
            models.add(primaryModel.trim());
        }
        if (fallbackModel != null && !fallbackModel.isBlank()) {
            String fallback = fallbackModel.trim();
            if (!models.contains(fallback)) {
                models.add(fallback);
            }
        }

        if (models.isEmpty()) {
            models.add("gemini-2.5-flash");
        }

        return models;
    }

    private String parseGeminiResponse(ResponseEntity<Map> response) {
        try {
            if (response.getBody() == null) {
                return "ERROR";
            }

            List candidates = (List) response.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "ERROR";
            }

            Map first = (Map) candidates.get(0);
            Map contentMap = (Map) first.get("content");
            List parts = (List) contentMap.get("parts");
            Map textPart = (Map) parts.get(0);

            return ((String) textPart.get("text")).trim();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    /**
     * ✅ 요약
     */
    public String summarize(String content) {

        String prompt = """
                너는 대학교 공지사항 요약 시스템이다.

                조건:
                - 5줄 이내
                - 핵심 일정 반드시 포함
                - 마감일 강조
                - 숫자 정보 빠짐없이 포함
                - 간결하고 정보 중심

                공지 내용:
                """ + content;

        return callGemini(prompt);
    }

    /**
     * 🔥 마감일 추출 (안정 버전)
     */
    public LocalDate extractDeadline(String content) {

        String prompt = """
                아래 공지에서 신청 마감일 또는 종료일을 찾아라.

                출력 형식:
                YYYY-MM-DD

                마감일이 없으면:
                NONE

                오늘 날짜는 2026-03-04이다.
                연도가 없으면 2026으로 간주해라.

                공지:
                """ + content;

        String result = callGemini(prompt);

        if (result.equalsIgnoreCase("NONE") || result.equals("ERROR"))
            return null;

        try {
            return LocalDate.parse(result.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
