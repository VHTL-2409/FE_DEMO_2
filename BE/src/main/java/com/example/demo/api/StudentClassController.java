package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.classmanagement.ClassResponse;
import com.example.demo.api.dto.classmanagement.ClassStudentResponse;
import com.example.demo.api.dto.classmanagement.JoinClassByCodeRequest;
import com.example.demo.service.ClassService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.domain.entity.User;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/classes")
@PreAuthorize("hasRole('STUDENT')")
public class StudentClassController {

    private final ClassService classService;
    private final CurrentUserService currentUserService;

    public StudentClassController(ClassService classService, CurrentUserService currentUserService) {
        this.classService = classService;
        this.currentUserService = currentUserService;
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
}
