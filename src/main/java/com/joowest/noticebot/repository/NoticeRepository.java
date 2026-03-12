package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, String> {

    List<Notice> findByDeadlineAndReminderSentFalse(LocalDate deadline);
    List<Notice> findTop10ByOrderByCreatedAtDesc();
    List<Notice> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    List<Notice> findTop10ByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
    List<Notice> findTop10ByDepartmentCodeIgnoreCaseOrderByCreatedAtDesc(String departmentCode);
}
