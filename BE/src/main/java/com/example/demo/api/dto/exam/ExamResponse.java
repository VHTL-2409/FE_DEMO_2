package com.example.demo.api.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExamResponse {
    private Long id;
    private String code;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Boolean isActive;
    private String createdBy;
    private Long questionCount;
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
