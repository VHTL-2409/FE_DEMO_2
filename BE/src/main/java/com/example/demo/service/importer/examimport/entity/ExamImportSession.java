package com.example.demo.service.importer.examimport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "exam_import_sessions",
        indexes = {
                @Index(name = "idx_eis_user_created", columnList = "user_id,created_at"),
                @Index(name = "idx_eis_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamImportSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private com.example.demo.domain.entity.User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private com.example.demo.domain.entity.Exam exam;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "template_used", length = 64)
    private String templateUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parse_result", columnDefinition = "jsonb")
    private String parseResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_data", columnDefinition = "jsonb")
    private String reportData;

    @Column(name = "python_service_url", length = 255)
    private String pythonServiceUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum SessionStatus {
        UPLOADED,
        PARSING,
        DONE,
        CONFIRMED,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = SessionStatus.UPLOADED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
