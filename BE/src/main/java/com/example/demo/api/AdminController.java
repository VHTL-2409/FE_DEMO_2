package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.admin.AdminClassListPageResponse;
import com.example.demo.api.dto.admin.AdminDashboardStatsResponse;
import com.example.demo.api.dto.admin.AdminExamActiveRequest;
import com.example.demo.api.dto.admin.AdminExamListPageResponse;
import com.example.demo.api.dto.admin.AdminUserDetailResponse;
import com.example.demo.api.dto.admin.AdminUserListPageResponse;
import com.example.demo.service.AdminDashboardService;
import com.example.demo.service.AdminExamManagementService;
import com.example.demo.service.AdminUserDeletionService;
import com.example.demo.service.AdminUserManagementService;
import com.example.demo.service.ClassService;
import com.example.demo.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserManagementService adminUserManagementService;
    private final AdminUserDeletionService adminUserDeletionService;
    private final AdminExamManagementService adminExamManagementService;
    private final ClassService classService;
    private final CurrentUserService currentUserService;

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDashboardStatsResponse> dashboardStats() {
        return ApiResponse.success(adminDashboardService.getDashboardStats());
    }

    @GetMapping("/ops/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> opsSummary() {
        return ApiResponse.success(adminDashboardService.getOpsSummary());
    }

    @GetMapping("/users/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserListPageResponse> listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.success(adminUserManagementService.listStudents(page, size, q));
    }

    @GetMapping("/users/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserDetailResponse> getStudentDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(adminUserManagementService.getStudentDetail(id));
    }

    @DeleteMapping("/users/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteStudent(@PathVariable("id") Long id) {
        adminUserDeletionService.deleteStudent(id, currentUserService.requireCurrentUser());
        return ApiResponse.success(null, "Đã xóa học sinh");
    }

    @GetMapping("/users/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserListPageResponse> listTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.success(adminUserManagementService.listTeachers(page, size, q));
    }

    @GetMapping("/users/teachers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserDetailResponse> getTeacherDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(adminUserManagementService.getTeacherDetail(id));
    }

    @DeleteMapping("/users/teachers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteTeacher(@PathVariable("id") Long id) {
        adminUserDeletionService.deleteTeacher(id, currentUserService.requireCurrentUser());
        return ApiResponse.success(null, "Đã xóa giáo viên");
    }

    @GetMapping("/users/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserListPageResponse> listAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminUserManagementService.listAdmins(page, size));
    }

    @GetMapping("/classes")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminClassListPageResponse> listClasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(classService.listAllClassesForAdmin(page, size));
    }

    @GetMapping("/exams")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminExamListPageResponse> listExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminExamManagementService.listExams(page, size));
    }

    @PatchMapping("/exams/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> setExamActive(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminExamActiveRequest request
    ) {
        if (request.getActive() == null) {
            return ApiResponse.error("Thiếu trường active");
        }
        adminExamManagementService.setExamActive(id, request.getActive());
        return ApiResponse.success(Map.of("id", id, "active", request.getActive()), "Đã cập nhật trạng thái đề thi");
    }
}
