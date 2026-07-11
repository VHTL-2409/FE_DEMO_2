package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.classmanagement.*;
import com.example.demo.api.dto.exam.ExamResponse;
import com.example.demo.service.ClassService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/classes")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class ClassController {

    private final ClassService classService;
    private final CurrentUserService currentUserService;
    private final ExamService examService;

    public ClassController(ClassService classService, CurrentUserService currentUserService, ExamService examService) {
        this.classService = classService;
        this.currentUserService = currentUserService;
        this.examService = examService;
    }

    @GetMapping
    public ApiResponse<List<ClassResponse>> listClasses() {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.getTeacherClasses(teacher));
    }

    @PostMapping
    public ApiResponse<ClassResponse> createClass(@Valid @RequestBody CreateClassRequest request) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.createClass(request, teacher), "Tạo lớp học thành công");
    }

    @PostMapping("/create-with-roster")
    public ApiResponse<ClassResponse> createClassWithRoster(
            @RequestPart("name") String name,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "subject", required = false) String subject,
            @RequestPart("file") MultipartFile file) {
        var teacher = currentUserService.requireCurrentUser();
        CreateClassRequest request = new CreateClassRequest();
        request.setName(name);
        request.setDescription(description);
        request.setSubject(subject);
        return ApiResponse.success(
                classService.createClassWithRoster(request, file, teacher),
                "Create class and import roster successfully");
    }

    @GetMapping("/{classId}")
    public ApiResponse<ClassResponse> getClass(@PathVariable Long classId) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.getClassDetail(classId, teacher));
    }

    @PutMapping("/{classId}")
    public ApiResponse<ClassResponse> updateClass(@PathVariable Long classId,
                                                   @Valid @RequestBody UpdateClassRequest request) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.updateClass(classId, request, teacher), "Cập nhật lớp học thành công");
    }

    @DeleteMapping("/{classId}")
    public ApiResponse<Void> deleteClass(@PathVariable Long classId) {
        var teacher = currentUserService.requireCurrentUser();
        classService.deleteClass(classId, teacher);
        return ApiResponse.success(null, "Xóa lớp học thành công");
    }

    @GetMapping("/{classId}/students")
    public ApiResponse<List<ClassStudentResponse>> getClassStudents(@PathVariable Long classId) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.getClassStudents(classId, teacher));
    }

    @GetMapping("/{classId}/exams")
    public ApiResponse<List<ExamResponse>> getClassExams(@PathVariable Long classId) {
        var actor = currentUserService.requireCurrentUser();
        classService.requireManageableClass(classId, actor);
        return ApiResponse.success(examService.listActiveExamsByClass(classId));
    }

    @PostMapping("/{classId}/students")
    public ApiResponse<List<ClassStudentResponse>> addStudents(@PathVariable Long classId,
                                                                @Valid @RequestBody AddStudentsRequest request) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.addStudentsToClass(classId, request, teacher), "Thêm học sinh thành công");
    }

    @PostMapping("/{classId}/students/force")
    public ApiResponse<List<ClassStudentResponse>> forceAddStudents(@PathVariable Long classId,
                                                                    @Valid @RequestBody ForceAddStudentsRequest request) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.forceAddStudentsToClass(classId, request, teacher), "Cưỡng chế thêm học sinh thành công");
    }

    @PostMapping("/{classId}/students/import")
    public ApiResponse<ImportStudentsResponse> importStudents(
            @PathVariable Long classId,
            @Valid @RequestBody StudentImportRequest request) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.importStudentsToClass(classId, request, teacher), "Import học sinh thành công");
    }

    
    @PostMapping("/{classId}/students/import-file")
    public ApiResponse<ImportStudentsResponse> importStudentsFile(
            @PathVariable Long classId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "mandatory", defaultValue = "true") boolean mandatory) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(
                classService.importStudentsFromFile(classId, file, mandatory, teacher),
                "Import học sinh từ file thành công");
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    public ApiResponse<Void> removeStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        var teacher = currentUserService.requireCurrentUser();
        classService.removeStudentFromClass(classId, studentId, teacher);
        return ApiResponse.success(null, "Xóa học sinh khỏi lớp thành công");
    }

    @GetMapping("/{classId}/available-students")
    public ApiResponse<List<ClassStudentResponse>> getAvailableStudents(@PathVariable Long classId,
                                                                        @RequestParam(required = false) String search) {
        var teacher = currentUserService.requireCurrentUser();
        return ApiResponse.success(classService.searchAvailableStudents(classId, teacher, search));
    }
}
