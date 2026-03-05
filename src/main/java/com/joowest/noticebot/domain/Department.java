package com.joowest.noticebot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    @Id
    private String id;

    @Indexed(unique = true)
    private String deptCode;

    private String deptName;
    private String noticeUrl;
    private Boolean enabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
