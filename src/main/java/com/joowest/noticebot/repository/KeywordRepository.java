package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.Keyword;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findByUserIdAndGuildSettingIdAndKeyword(Long userId, Long guildSettingId, String keyword);
    List<Keyword> findByUserIdAndGuildSettingId(Long userId, Long guildSettingId);
    List<Keyword> findByGuildSettingId(Long guildSettingId);
}
