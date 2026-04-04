package com.example.demo.service.importer.examimport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "exam_question_renders",
        indexes = {
                @Index(name = "idx_eqr_session_index", columnList = "session_id,question_index")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestionRender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamImportSession session;

    @Column(name = "question_index", nullable = false)
    private Integer questionIndex;

    @Column(name = "question_number")
    private Integer questionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "render_mode", nullable = false, length = 16)
    private RenderMode renderMode;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bbox", columnDefinition = "jsonb")
    private String bbox;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum RenderMode {
        TEXT,
        IMAGE
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (confidence == null) confidence = 1.0;
        if (renderMode == null) renderMode = RenderMode.TEXT;
    }
}
