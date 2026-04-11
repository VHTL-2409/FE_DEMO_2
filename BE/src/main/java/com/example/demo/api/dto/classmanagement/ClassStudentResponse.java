package com.example.demo.api.dto.classmanagement;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ClassStudentResponse {
    private Long id;
    private Long classId;
    private String className;
    private Long studentId;
    private String studentUsername;
    private String studentEmail;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime joinedAt;
}
