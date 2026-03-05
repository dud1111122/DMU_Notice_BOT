package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.UserSubscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends MongoRepository<UserSubscription, String> {
    Optional<UserSubscription> findByUserIdAndGuildIdAndDept(String userId, String guildId, String dept);
    List<UserSubscription> findByUserIdAndGuildId(String userId, String guildId);
    List<UserSubscription> findByGuildIdAndDeptAndEnabledTrue(String guildId, String dept);
}
