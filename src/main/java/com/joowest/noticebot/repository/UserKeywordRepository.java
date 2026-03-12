package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.UserKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {
    Optional<UserKeyword> findByUserIdAndGuildIdAndKeyword(String userId, String guildId, String keyword);
    List<UserKeyword> findByUserIdAndGuildId(String userId, String guildId);
}
