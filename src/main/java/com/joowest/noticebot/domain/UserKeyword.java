package com.joowest.noticebot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_keywords")
@CompoundIndex(name = "uniq_user_guild_keyword", def = "{'userId': 1, 'guildId': 1, 'keyword': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserKeyword {
    @Id
    private String id;
    private String userId;
    private String guildId;
    private String keyword;
    private LocalDateTime createdAt;
}
