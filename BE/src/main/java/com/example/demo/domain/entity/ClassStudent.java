package com.example.demo.domain.entity;

import com.example.demo.common.VietNamTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_students",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_class_student", columnNames = {"class_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_class_students_class", columnList = "class_id"),
                @Index(name = "idx_class_students_student", columnList = "student_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) joinedAt = VietNamTime.now();
    }
}
