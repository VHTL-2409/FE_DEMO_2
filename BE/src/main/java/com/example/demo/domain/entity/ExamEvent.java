package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "exam_events",
        indexes = {
                @Index(name = "idx_exam_events_attempt_type", columnList = "attempt_id,event_type"),
                @Index(name = "idx_exam_events_attempt_created", columnList = "attempt_id,created_at"),
                @Index(name = "idx_exam_events_fingerprint", columnList = "device_fingerprint")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private String eventData;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SignalSeverity severity;

    @Column(name = "device_fingerprint", length = 128)
    private String deviceFingerprint;

    @Column(name = "sequence_no")
    private Long sequenceNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
