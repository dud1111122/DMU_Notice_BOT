package com.joowest.noticebot.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeadlineParserService {

    private static final Pattern FULL_DATE_PATTERN =
            Pattern.compile("(\\d{4})\\s*[./-]\\s*(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})");
    private static final Pattern KOREAN_MONTH_DAY_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일(?:\\s*까지)?");
    private static final Pattern TILDE_MONTH_DAY_PATTERN =
            Pattern.compile("~\\s*(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})(?:\\s*까지)?");
    private static final Pattern MONTH_DAY_UNTIL_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})\\s*까지");

    private final GeminiService geminiService;

    public DeadlineParserService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public LocalDate extractDeadline(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        LocalDate regexDate = extractWithRegex(text);
        if (regexDate != null) {
            return regexDate;
        }

        // Regex로 찾지 못한 경우에만 Gemini 호출
        return geminiService.extractDeadline(text);
    }

    private LocalDate extractWithRegex(String text) {
        List<Pattern> patterns = List.of(
                FULL_DATE_PATTERN,
                KOREAN_MONTH_DAY_PATTERN,
                TILDE_MONTH_DAY_PATTERN,
                MONTH_DAY_UNTIL_PATTERN
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) {
                continue;
            }

            if (pattern == FULL_DATE_PATTERN) {
                Integer year = parseInt(matcher.group(1));
                Integer month = parseInt(matcher.group(2));
                Integer day = parseInt(matcher.group(3));
                LocalDate parsed = toDate(year, month, day);
                if (parsed != null) {
                    return parsed;
                }
            } else {
                Integer month = parseInt(matcher.group(1));
                Integer day = parseInt(matcher.group(2));
                int year = LocalDate.now().getYear();
                LocalDate parsed = toDate(year, month, day);
                if (parsed != null) {
                    return parsed;
                }
            }
        }

        return null;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate toDate(Integer year, Integer month, Integer day) {
        if (year == null || month == null || day == null) {
            return null;
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }
}
