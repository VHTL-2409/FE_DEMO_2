package com.example.demo.api.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ExamRequest {
    @NotBlank
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @NotNull
    @Positive
    private Integer durationMinutes;
    private Boolean isActive;
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
    private String className;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
}
