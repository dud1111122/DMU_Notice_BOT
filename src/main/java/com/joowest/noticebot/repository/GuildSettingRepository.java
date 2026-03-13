package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.GuildSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildSettingRepository extends JpaRepository<GuildSetting, Long> {
    Optional<GuildSetting> findByGuildId(String guildId);
    List<GuildSetting> findByEnabledTrue();
}
