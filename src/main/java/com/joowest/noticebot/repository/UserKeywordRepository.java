package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.UserKeyword;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserKeywordRepository extends MongoRepository<UserKeyword, String> {
    Optional<UserKeyword> findByUserIdAndGuildIdAndKeyword(String userId, String guildId, String keyword);
    List<UserKeyword> findByUserIdAndGuildId(String userId, String guildId);
}
