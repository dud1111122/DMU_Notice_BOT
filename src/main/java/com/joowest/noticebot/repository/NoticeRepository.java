package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.Notice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface NoticeRepository extends MongoRepository<Notice, String> {

    List<Notice> findByDeadlineAndReminderSentFalse(LocalDate deadline);
}