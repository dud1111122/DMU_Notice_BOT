package com.joowest.noticebot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String date;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String summary;
    private String category;
    private String dept;
    private LocalDateTime createdAt;
    private LocalDate deadline;

    private boolean reminderSent;

    public Notice() {}

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

    public void setId(String id) { this.id = id; }

    public void setTitle(String title) { this.title = title; }

    public void setDate(String date) { this.date = date; }

    public void setUrl(String url) { this.url = url; }

    public void setSummary(String summary) { this.summary = summary; }

    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}
