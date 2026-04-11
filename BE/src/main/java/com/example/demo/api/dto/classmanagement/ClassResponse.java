package com.example.demo.api.dto.classmanagement;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ClassResponse {
    private Long id;
    private String name;
    private String description;
    private String subject;
    private String classCode;
    private Long teacherId;
    private String teacherName;
    private Long studentCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime updatedAt;
}
