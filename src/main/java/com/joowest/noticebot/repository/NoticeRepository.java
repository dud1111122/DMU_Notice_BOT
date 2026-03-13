package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.Notice;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    boolean existsByDepartmentIdAndExternalId(Long departmentId, String externalId);
    List<Notice> findTop10ByOrderByPostedAtDescCreatedAtDesc();
    List<Notice> findByPostedAtBetweenOrderByPostedAtDescCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    List<Notice> findTop10ByTitleContainingIgnoreCaseOrderByPostedAtDescCreatedAtDesc(String keyword);
    List<Notice> findTop10ByDepartmentDeptCodeIgnoreCaseOrderByPostedAtDescCreatedAtDesc(String departmentCode);
}
