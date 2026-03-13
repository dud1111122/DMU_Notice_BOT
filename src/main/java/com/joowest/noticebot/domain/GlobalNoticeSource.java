package com.joowest.noticebot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "global_notice_sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalNoticeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_code", nullable = false, unique = true)
    private String sourceCode;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "notice_url", nullable = false, columnDefinition = "TEXT")
    private String noticeUrl;

    @Column(name = "last_seen_external_id")
    private String lastSeenExternalId;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (enabled == null) {
            enabled = true;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
