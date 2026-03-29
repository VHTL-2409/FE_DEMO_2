package com.example.demo.api.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExamResponse {
    private Long id;
    private String code;
    private String title;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endTime;
    private String timezone;
    private Integer durationMinutes;
    private Boolean isActive;
    private String createdBy;
    private Long questionCount;
    /** Số thí sinh đã có ít nhất một lượt làm bài (distinct theo user) */
    private Long participantCount;
    private Boolean monitorTabSwitch;
    private Boolean monitorBlur;
    private Boolean monitorExitFullscreen;
    private Boolean monitorCopyPaste;
    private Boolean monitorIdleTime;
    private Boolean monitorDevtools;
    private Boolean monitorDuplicateIp;
    private Boolean monitorFastSubmit;
    private Boolean monitorRightClick;
    private Boolean monitorPrintScreen;
    private Boolean monitorRapidQuestionSwitch;
    private Boolean monitorMultiMonitor;
    private Boolean requireCameraMic;
}
