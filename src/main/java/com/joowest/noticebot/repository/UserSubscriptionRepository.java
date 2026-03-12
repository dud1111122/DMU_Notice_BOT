package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByUserIdAndGuildIdAndDepartmentCode(String userId, String guildId, String departmentCode);
    List<UserSubscription> findByUserIdAndGuildId(String userId, String guildId);
    List<UserSubscription> findByGuildIdAndDepartmentCodeAndEnabledTrue(String guildId, String departmentCode);
}
