package com.joowest.noticebot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "guild_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuildSetting {
    @Id
    private String id;
    private String guildId;
    private String channelId;
}
