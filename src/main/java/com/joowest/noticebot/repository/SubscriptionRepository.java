package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.Subscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndGuildSettingIdAndDepartmentId(Long userId, Long guildSettingId, Long departmentId);
    List<Subscription> findByUserIdAndGuildSettingId(Long userId, Long guildSettingId);
    List<Subscription> findByGuildSettingIdAndDepartmentIdAndEnabledTrue(Long guildSettingId, Long departmentId);
}
