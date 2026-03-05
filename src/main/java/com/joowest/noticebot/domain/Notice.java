package com.joowest.noticebot.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "notices")
public class Notice {

    @Id
    private String id;

    private String title;
    private String date;
    private String url;
    private String summary;
    private String category;
    private String dept;
    private LocalDateTime createdAt;
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
        this.category = null;
        this.dept = null;
        this.createdAt = LocalDateTime.now();
        this.deadline = deadline;
        this.reminderSent = false;
    }

    public Notice(String id,
                  String title,
                  String date,
                  String url,
                  String summary,
                  String category,
                  String dept,
                  LocalDate deadline) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.url = url;
        this.summary = summary;
        this.category = category;
        this.dept = dept;
        this.createdAt = LocalDateTime.now();
        this.deadline = deadline;
        this.reminderSent = false;
    }

    // 🔥 Getter / Setter

    public String getId() { return id; }

    public String getTitle() { return title; }

    public String getDate() { return date; }

    public String getUrl() { return url; }

    public String getSummary() { return summary; }

    public String getCategory() { return category; }

    public String getDept() { return dept; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDate getDeadline() { return deadline; }

    public boolean isReminderSent() { return reminderSent; }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}
