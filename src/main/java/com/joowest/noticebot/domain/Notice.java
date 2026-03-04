package com.joowest.noticebot.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "notices")
public class Notice {

    @Id
    private String id;

    private String title;
    private String date;
    private String url;
    private String summary;
    private LocalDate deadline;

    private boolean reminderSent;

    // 🔥 기본 생성자 (Mongo 필수)
    public Notice() {}

    // 🔥 생성자 (reminderSent 자동 false)
    public Notice(String id,
                  String title,
                  String date,
                  String url,
                  String summary,
                  LocalDate deadline) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.url = url;
        this.summary = summary;
        this.deadline = deadline;
        this.reminderSent = false;
    }

    // 🔥 Getter / Setter

    public String getId() { return id; }

    public String getTitle() { return title; }

    public String getDate() { return date; }

    public String getUrl() { return url; }

    public String getSummary() { return summary; }

    public LocalDate getDeadline() { return deadline; }

    public boolean isReminderSent() { return reminderSent; }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}