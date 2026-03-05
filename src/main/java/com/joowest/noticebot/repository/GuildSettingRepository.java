package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.GuildSetting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuildSettingRepository extends MongoRepository<GuildSetting, String> {
    GuildSetting findByGuildId(String guildId);
    List<GuildSetting> findByEnabledTrue();
}
