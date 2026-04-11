package com.example.demo.api.dto.monitoring;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProctoringTelemetry {

    private Long blurDurationMs;

    private Long hiddenDurationMs;

    private Integer fullscreenExitCount;

    private Long fullscreenReentryDelayMs;

    private Long questionDwellMs;

    private Long questionSwitchSpanMs;

    private Integer questionIndex;

    private Long questionId;

    private Integer pasteLength;

    private Integer copiedTextLength;

    @Size(max = 64)
    private String selectionSource;

    private Boolean networkOnline;

    private Integer reconnectCount;

    private Long heartbeatLagMs;

    private Long offlineDurationMs;

    private Integer answerChangeCount;

    private Integer clipboardBurstCount;

    private Long focusRecoveryDelayMs;

    @Size(max = 64)
    private String eventSource;

    @Size(max = 64)
    private String networkType;

    private Boolean multipleWindowsDetected;

    private Boolean screenShareActive;
}
