package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "import_job_issue",
        indexes = {
                @Index(name = "idx_import_job_issue_job_created", columnList = "job_id,created_at"),
                @Index(name = "idx_import_job_issue_resolved", columnList = "resolved")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(name = "question_index")
    private Integer questionIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issue_data", columnDefinition = "jsonb")
    private String issueData;

    @Column(nullable = false)
    private Boolean resolved;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
