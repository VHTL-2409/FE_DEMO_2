package com.example.demo.api.dto.submission;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DraftAnswersResponse {
    private Long attemptId;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime deadlineAt;
    private Long remainingSeconds;
    private List<DraftAnswerItem> answers;
}
