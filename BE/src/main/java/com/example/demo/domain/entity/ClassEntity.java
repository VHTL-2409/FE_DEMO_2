package com.example.demo.domain.entity;

import com.example.demo.common.VietNamTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "classes",
        indexes = {
                @Index(name = "idx_classes_teacher", columnList = "teacher_id"),
                @Index(name = "idx_classes_teacher_name", columnList = "teacher_id, name", unique = true),
                @Index(name = "idx_classes_code", columnList = "class_code", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String subject;

    @Column(name = "class_code", length = 8, unique = true)
    private String classCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = VietNamTime.now();
        if (updatedAt == null) updatedAt = VietNamTime.now();
        if (classCode == null) classCode = generateClassCode();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = VietNamTime.now();
    }

    private String generateClassCode() {
        String uuid = UUID.randomUUID().toString().toUpperCase();
        return uuid.replaceAll("-", "").substring(0, 6);
    }
}
