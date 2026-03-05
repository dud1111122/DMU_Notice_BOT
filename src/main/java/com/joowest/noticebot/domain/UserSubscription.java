package com.joowest.noticebot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_subscriptions")
@CompoundIndex(name = "uniq_user_guild_dept", def = "{'userId': 1, 'guildId': 1, 'dept': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {
    @Id
    private String id;
    private String userId;
    private String guildId;
    private String dept;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
