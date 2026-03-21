package com.example.demo.service;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.EmailVerificationTokenRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.RiskSnapshotRepository;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.repository.TeacherProfileRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserDeletionService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamService examService;
    private final ExamAttemptRepository examAttemptRepository;
    private final AnswerRepository answerRepository;
    private final AuditLogRepository auditLogRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final MonitoringEventRepository monitoringEventRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional
    public void deleteStudent(Long userId, User currentAdmin) {
        if (userId.equals(currentAdmin.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể xóa chính tài khoản đang đăng nhập");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        if (!hasRole(user, RoleName.STUDENT)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản không phải học sinh");
        }

        List<ExamAttempt> attempts = examAttemptRepository.findByStudent(user);
        if (!attempts.isEmpty()) {
            riskSnapshotRepository.deleteByAttemptIn(attempts);
            monitoringEventRepository.deleteByAttemptIn(attempts);
            auditLogRepository.deleteByAttemptIn(attempts);
            answerRepository.deleteByAttemptIn(attempts);
            examAttemptRepository.deleteAll(attempts);
        }

        studentProfileRepository.deleteByUser(user);
        emailVerificationTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        user.getRoles().clear();
        userRepository.save(user);
        userRepository.delete(user);
    }

    @Transactional
    public void deleteTeacher(Long userId, User currentAdmin) {
        if (userId.equals(currentAdmin.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể xóa chính tài khoản đang đăng nhập");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        if (!hasRole(user, RoleName.TEACHER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản không phải giáo viên");
        }

        List<Exam> exams = examRepository.findByCreatedById(user.getId());
        for (Exam e : exams) {
            examService.deleteExam(e.getId(), currentAdmin);
        }

        teacherProfileRepository.deleteByUser(user);
        emailVerificationTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        user.getRoles().clear();
        userRepository.save(user);
        userRepository.delete(user);
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == roleName);
    }
}
