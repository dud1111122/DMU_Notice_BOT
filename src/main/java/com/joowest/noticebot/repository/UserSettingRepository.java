package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.UserSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUserIdAndGuildSettingId(Long userId, Long guildSettingId);
    java.util.List<UserSetting> findByGuildSettingIdAndAllNoticeEnabledTrue(Long guildSettingId);
}
