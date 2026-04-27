package com.example.demo.service;

import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.ProctorFlag;
import com.example.demo.domain.entity.ProctorFlagStatus;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ProctorFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý Proctor Flags.
 * Việc tạo/đồng bộ flag được xử lý bởi RiskScoringService.syncRiskFlag().
 * Service này chỉ quản lý các hành động review.
 */
@Service
@RequiredArgsConstructor
public class ProctorFlagService {

    private final ProctorFlagRepository proctorFlagRepository;

    /**
     * Tìm flag đang mở (OPEN) của một attempt.
     */
    @Transactional(readOnly = true)
    public ProctorFlag findActiveFlag(ExamAttempt attempt) {
        return proctorFlagRepository
                .findFirstByAttemptAndStatusOrderByCreatedAtDesc(attempt, ProctorFlagStatus.OPEN)
                .orElse(null);
    }

    /**
     * Thực hiện review một flag: cập nhật trạng thái, ghi nhận người review và ghi chú.
     */
    @Transactional
    public ProctorFlag review(ProctorFlag flag, ProctorFlagStatus status, String teacherNote, User reviewer) {
        flag.setStatus(status);
        flag.setTeacherNote(teacherNote == null || teacherNote.isBlank() ? null : teacherNote.trim());
        flag.setReviewedBy(reviewer);
        flag.setReviewedAt(VietNamTime.now());
        return proctorFlagRepository.save(flag);
    }
}
