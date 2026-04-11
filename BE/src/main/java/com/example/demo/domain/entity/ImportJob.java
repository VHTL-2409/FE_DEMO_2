package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "import_jobs",
        indexes = {
                @Index(name = "idx_import_jobs_owner_created", columnList = "owner_id,created_at"),
                @Index(name = "idx_import_jobs_exam_created", columnList = "exam_id,created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @Column(name = "source_file_name", nullable = false, length = 255)
    private String sourceFileName;

    @Column(name = "file_type", nullable = false, length = 40)
    private String fileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportJobStatus status;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parse_summary", columnDefinition = "jsonb")
    private String parseSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_data", columnDefinition = "jsonb")
    private String parsedData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_log", columnDefinition = "jsonb")
    private String errorLog;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
