package com.joowest.noticebot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
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

    @Column(nullable = false)
    private String departmentCode;

    @Column(nullable = false)
    private String departmentName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDate deadline;

    private boolean reminderSent;

    public Notice(String id,
                  String title,
                  String date,
                  String url,
                  String summary,
                  String departmentCode,
                  String departmentName,
                  LocalDate deadline) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.url = url;
        this.summary = summary;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.deadline = deadline;
        this.reminderSent = false;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
