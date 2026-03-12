package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.GuildSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuildSettingRepository extends JpaRepository<GuildSetting, Long> {
    GuildSetting findByGuildId(String guildId);
    List<GuildSetting> findByEnabledTrue();
}
