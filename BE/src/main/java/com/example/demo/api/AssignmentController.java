package com.example.demo.api;

import com.example.demo.api.dto.assignment.AssignmentRequest;
import com.example.demo.api.dto.assignment.AssignmentResponse;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AssignmentService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams/{examId}/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ExamService examService;
    private final CurrentUserService currentUserService;

    public AssignmentController(AssignmentService assignmentService,
                                ExamService examService,
                                CurrentUserService currentUserService) {
        this.assignmentService = assignmentService;
        this.examService = examService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<AssignmentResponse> list(@PathVariable Long examId) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return assignmentService.listByExam(exam);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public AssignmentResponse create(@PathVariable Long examId,
                                     @Valid @RequestBody AssignmentRequest request) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return assignmentService.create(exam, actor, request);
    }
}
