package com.example.demo.api;

import com.example.demo.api.dto.assignment.AssignmentRequest;
import com.example.demo.api.dto.assignment.AssignmentResponse;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AssignmentService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import jakarta.validation.Valid;
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
    public List<AssignmentResponse> list(@PathVariable Long examId) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return assignmentService.listByExam(exam);
    }

    @GetMapping("/public")
    public List<AssignmentResponse> listPublished(@PathVariable Long examId) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireAccessibleExam(examId, actor);
        return assignmentService.listPublishedByExam(exam);
    }

    @PostMapping
    public AssignmentResponse create(@PathVariable Long examId,
                                     @Valid @RequestBody AssignmentRequest request) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return assignmentService.create(exam, actor, request);
    }

    @PutMapping("/{assignmentId}")
    public AssignmentResponse update(@PathVariable Long examId,
                                     @PathVariable Long assignmentId,
                                     @Valid @RequestBody AssignmentRequest request) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return assignmentService.update(exam, assignmentId, request);
    }

    @PatchMapping("/{assignmentId}/publish")
    public AssignmentResponse publish(@PathVariable Long examId,
                                      @PathVariable Long assignmentId,
                                      @RequestParam Boolean isPublished) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return assignmentService.updatePublished(exam, assignmentId, isPublished);
    }

    @DeleteMapping("/{assignmentId}")
    public void delete(@PathVariable Long examId,
                       @PathVariable Long assignmentId) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        assignmentService.delete(exam, assignmentId);
    }
}
