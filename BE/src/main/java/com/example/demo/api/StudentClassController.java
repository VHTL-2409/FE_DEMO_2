package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.classmanagement.ClassResponse;
import com.example.demo.api.dto.classmanagement.ClassStudentResponse;
import com.example.demo.api.dto.classmanagement.JoinClassByCodeRequest;
import com.example.demo.api.dto.exam.ExamResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ClassRepository;
import com.example.demo.service.ClassService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/classes")
@PreAuthorize("hasRole('STUDENT')")
public class StudentClassController {

    private final ClassService classService;
    private final CurrentUserService currentUserService;
    private final ExamService examService;
    private final ClassRepository classRepository;

    public StudentClassController(ClassService classService, CurrentUserService currentUserService,
                                  ExamService examService, ClassRepository classRepository) {
        this.classService = classService;
        this.currentUserService = currentUserService;
        this.examService = examService;
        this.classRepository = classRepository;
    }

    @PostMapping("/join")
    public ApiResponse<ClassStudentResponse> joinClassByCode(@Valid @RequestBody JoinClassByCodeRequest request) {
        User student = currentUserService.requireCurrentUser();
        ClassStudentResponse result = classService.joinClassByCode(request.getClassCode(), student);
        return ApiResponse.success(result, "Tham gia lớp học thành công");
    }

    @GetMapping("/my-classes")
    public ApiResponse<List<ClassResponse>> getMyClasses() {
        User student = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.getStudentClasses(student));
    }

    @DeleteMapping("/{classId}/leave")
    public ApiResponse<Void> leaveClass(@PathVariable Long classId) {
        User student = currentUserService.requireCurrentUser();
        classService.leaveClass(classId, student);
        return ApiResponse.success(null, "Rời lớp học thành công");
    }

    @GetMapping("/{classId}/exams")
    public ApiResponse<List<ExamResponse>> getClassExams(@PathVariable Long classId) {
        User student = currentUserService.requireCurrentUser();
        classRepository.findById(classId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học"));
        if (!classService.isStudentEnrolled(classId, student.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp học này");
        }
        return ApiResponse.success(examService.listActiveExamsByClass(classId));
    }
}
