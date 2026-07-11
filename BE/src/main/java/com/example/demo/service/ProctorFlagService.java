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


@Service
@RequiredArgsConstructor
public class ProctorFlagService {

    private final ProctorFlagRepository proctorFlagRepository;

    

    @Transactional(readOnly = true)
    public ProctorFlag findActiveFlag(ExamAttempt attempt) {
        return proctorFlagRepository
                .findFirstByAttemptAndStatusOrderByCreatedAtDesc(attempt, ProctorFlagStatus.OPEN)
                .orElse(null);
    }

    

    @Transactional
    public ProctorFlag review(ProctorFlag flag, ProctorFlagStatus status, String teacherNote, User reviewer) {
        flag.setStatus(status);
        flag.setTeacherNote(teacherNote == null || teacherNote.isBlank() ? null : teacherNote.trim());
        flag.setReviewedBy(reviewer);
        flag.setReviewedAt(VietNamTime.now());
        return proctorFlagRepository.save(flag);
    }
}
