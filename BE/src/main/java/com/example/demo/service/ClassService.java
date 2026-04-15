package com.example.demo.service;

import com.example.demo.api.dto.admin.AdminClassListPageResponse;
import com.example.demo.api.dto.classmanagement.*;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.ClassEntity;
import com.example.demo.domain.entity.ClassStudent;
import com.example.demo.domain.entity.Role;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ClassRepository;
import com.example.demo.repository.ClassStudentRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private static OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(VN).toOffsetDateTime();
    }

    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final StudentImportFileParser studentImportFileParser;

    @Transactional(readOnly = true)
    public List<ClassResponse> getTeacherClasses(User teacher) {
        List<ClassEntity> classes = classRepository.findByTeacherId(teacher.getId());
        return classes.stream()
                .map(this::toClassResponse)
                .collect(Collectors.toList());
    }

    /** Danh sách lớp toàn hệ thống (admin / giáo vụ). */
    @Transactional(readOnly = true)
    public AdminClassListPageResponse listAllClassesForAdmin(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ClassEntity> pg = classRepository.findAll(pageable);
        List<ClassResponse> content = pg.getContent().stream()
                .map(this::toClassResponse)
                .collect(Collectors.toList());
        return AdminClassListPageResponse.builder()
                .content(content)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .totalPages(pg.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public ClassResponse getClassDetail(Long classId, User teacher) {
        ClassEntity classEntity = requireManageableClass(classId, teacher);
        return toClassResponse(classEntity);
    }

    @Transactional
    public ClassResponse createClass(CreateClassRequest request, User teacher) {
        currentUserService.requireTeacherOrAdmin(teacher);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tên lớp không được để trống");
        }

        if (classRepository.existsByTeacherIdAndName(teacher.getId(), request.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Lớp học với tên này đã tồn tại");
        }

        ClassEntity classEntity = ClassEntity.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .subject(request.getSubject() != null ? request.getSubject().trim() : null)
                .teacher(teacher)
                .build();

        classEntity = classRepository.save(classEntity);
        return toClassResponse(classEntity);
    }

    @Transactional
    public ClassResponse updateClass(Long classId, UpdateClassRequest request, User teacher) {
        ClassEntity classEntity = requireManageableClass(classId, teacher);
        String previousName = classEntity.getName();

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!request.getName().trim().equals(classEntity.getName()) &&
                classRepository.existsByTeacherIdAndName(teacher.getId(), request.getName().trim())) {
                throw new ApiException(HttpStatus.CONFLICT, "Lớp học với tên này đã tồn tại");
            }
            classEntity.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            classEntity.setDescription(request.getDescription().trim());
        }

        if (request.getSubject() != null) {
            classEntity.setSubject(request.getSubject().trim());
        }

        classEntity = classRepository.save(classEntity);
        syncExamClassName(previousName, classEntity.getName(), teacher);
        return toClassResponse(classEntity);
    }

    @Transactional
    public void deleteClass(Long classId, User teacher) {
        ClassEntity classEntity = requireManageableClass(classId, teacher);
        classStudentRepository.deleteByClassEntity(classEntity);
        classRepository.delete(classEntity);
    }

    @Transactional(readOnly = true)
    public List<ClassStudentResponse> getClassStudents(Long classId, User teacher) {
        ClassEntity classEntity = requireManageableClass(classId, teacher);
        List<ClassStudent> classStudents = classStudentRepository.findByClassEntity(classEntity);
        return classStudents.stream()
                .map(this::toClassStudentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ClassStudentResponse> addStudentsToClass(Long classId, AddStudentsRequest request, User teacher) {
        ClassEntity classEntity = requireManageableClass(classId, teacher);

        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Danh sách học sinh không được để trống");
        }

        for (Long studentId : request.getStudentIds()) {
            if (classStudentRepository.existsByClassEntityIdAndStudentId(classId, studentId)) {
                continue;
            }

            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh với ID: " + studentId));

            boolean isStudent = student.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.STUDENT);
            if (!isStudent) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Người dùng " + student.getUsername() + " không phải là học sinh");
            }

            ClassStudent classStudent = ClassStudent.builder()
                    .classEntity(classEntity)
                    .student(student)
                    .build();
            classStudentRepository.save(classStudent);
        }

        List<ClassStudent> classStudents = classStudentRepository.findByClassEntity(classEntity);
        return classStudents.stream()
                .map(this::toClassStudentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeStudentFromClass(Long classId, Long studentId, User teacher) {
        requireManageableClass(classId, teacher);

        ClassStudent classStudent = classStudentRepository.findByClassEntityIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Học sinh không có trong lớp này"));

        classStudentRepository.delete(classStudent);
    }

    @Transactional(readOnly = true)
    public List<ClassStudentResponse> getAvailableStudents(Long classId, User teacher) {
        requireManageableClass(classId, teacher);

        List<User> availableStudents = classStudentRepository.findAvailableStudents(classId);
        return mapAvailableStudents(availableStudents);
    }

    @Transactional(readOnly = true)
    public List<ClassStudentResponse> searchAvailableStudents(Long classId, User teacher, String keyword) {
        requireManageableClass(classId, teacher);

        List<User> availableStudents = (keyword == null || keyword.isBlank())
                ? classStudentRepository.findAvailableStudents(classId)
                : classStudentRepository.findAvailableStudentsByKeyword(classId, keyword.trim());
        return mapAvailableStudents(availableStudents);
    }

    @Transactional
    public void leaveClass(Long classId, User student) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học"));

        ClassStudent classStudent = classStudentRepository.findByClassEntityAndStudent(classEntity, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bạn không thuộc lớp học này"));

        classStudentRepository.delete(classStudent);
    }

    private List<ClassStudentResponse> mapAvailableStudents(List<User> availableStudents) {
        return availableStudents.stream()
                .map(student -> ClassStudentResponse.builder()
                        .studentId(student.getId())
                        .studentUsername(student.getUsername())
                        .studentEmail(student.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ClassStudentResponse joinClassByCode(String classCode, User student) {
        if (classCode == null || classCode.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã lớp không được để trống");
        }

        ClassEntity classEntity = classRepository.findByClassCode(classCode.toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học với mã: " + classCode));

        if (classStudentRepository.existsByClassEntityIdAndStudentId(classEntity.getId(), student.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Bạn đã tham gia lớp này rồi");
        }

        ClassStudent classStudent = ClassStudent.builder()
                .classEntity(classEntity)
                .student(student)
                .build();
        classStudent = classStudentRepository.save(classStudent);

        return toClassStudentResponse(classStudent);
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> getStudentClasses(User student) {
        List<ClassStudent> classStudents = classStudentRepository.findByStudent(student);
        return classStudents.stream()
                .map(cs -> toClassResponse(cs.getClassEntity()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(Long classId, Long studentId) {
        return classStudentRepository.existsByClassEntityIdAndStudentId(classId, studentId);
    }

    @Transactional(readOnly = true)
    public Optional<ClassEntity> findTeacherClassByName(User teacher, String className) {
        if (teacher == null || className == null || className.isBlank()) {
            return Optional.empty();
        }
        return classRepository.findByTeacherIdAndNameIgnoreCase(teacher.getId(), className.trim());
    }

    @Transactional(readOnly = true)
    public Optional<ClassEntity> findStudentEnrolledClassForExam(User student, com.example.demo.domain.entity.Exam exam) {
        if (student == null || exam == null || exam.getCreatedBy() == null) {
            return Optional.empty();
        }
        String className = exam.getClassName();
        if (className == null || className.isBlank()) {
            return Optional.empty();
        }
        String normalizedClassName = className.trim().toLowerCase(Locale.ROOT);
        Long teacherId = exam.getCreatedBy().getId();
        return classStudentRepository.findByStudentIdWithClassAndTeacher(student.getId()).stream()
                .map(ClassStudent::getClassEntity)
                .filter(classEntity -> classEntity != null && classEntity.getTeacher() != null)
                .filter(classEntity -> teacherId.equals(classEntity.getTeacher().getId()))
                .filter(classEntity -> {
                    String candidateName = classEntity.getName();
                    return candidateName != null && candidateName.trim().toLowerCase(Locale.ROOT).equals(normalizedClassName);
                })
                .findFirst();
    }

    @Transactional
    public List<ClassStudentResponse> forceAddStudentsToClass(Long classId, ForceAddStudentsRequest request, User teacher) {
        ClassEntity classEntity = requireManageableClass(classId, teacher);

        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Danh sách học sinh không được để trống");
        }

        int addedCount = 0;
        int updatedCount = 0;

        for (Long studentId : request.getStudentIds()) {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh với ID: " + studentId));

            boolean isStudent = student.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.STUDENT);
            if (!isStudent) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Người dùng " + student.getUsername() + " không phải là học sinh");
            }

            Optional<ClassStudent> existing = classStudentRepository.findByClassEntityIdAndStudentId(classId, studentId);
            if (existing.isPresent()) {
                ClassStudent classStudent = existing.get();
                classStudent.setJoinedAt(java.time.LocalDateTime.now());
                classStudentRepository.save(classStudent);
                updatedCount++;
            } else {
                ClassStudent classStudent = ClassStudent.builder()
                        .classEntity(classEntity)
                        .student(student)
                        .build();
                classStudentRepository.save(classStudent);
                addedCount++;
            }
        }

        List<ClassStudent> classStudents = classStudentRepository.findByClassEntity(classEntity);
        return classStudents.stream()
                .map(this::toClassStudentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ImportStudentsResponse importStudentsToClass(Long classId, StudentImportRequest request, User teacher) {
        ClassEntity classEntity = requireManageableClass(classId, teacher);

        List<StudentImportResult> results = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

        for (StudentImportItem item : request.getStudents()) {
            try {
                StudentImportResult result = processStudentImport(item, classEntity, request.isMandatory());
                results.add(result);
                if (result.isSuccess()) {
                    if (result.isNew()) {
                        createdCount++;
                    } else {
                        updatedCount++;
                    }
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                results.add(StudentImportResult.builder()
                        .username(item.getUsername())
                        .success(false)
                        .message(e.getMessage())
                        .build());
                failedCount++;
            }
        }

        return ImportStudentsResponse.builder()
                .totalReceived(request.getStudents().size())
                .successCount(createdCount + updatedCount)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }

    @Transactional
    public ImportStudentsResponse importStudentsFromFile(Long classId, MultipartFile file, boolean mandatory, User teacher) {
        List<StudentImportItem> students = studentImportFileParser.parse(file);
        if (students.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "File không có dòng học sinh hợp lệ (cần cột username, tối thiểu 3 ký tự).");
        }
        StudentImportRequest request = new StudentImportRequest();
        request.setStudents(students);
        request.setMandatory(mandatory);
        return importStudentsToClass(classId, request, teacher);
    }

    private StudentImportResult processStudentImport(StudentImportItem item, ClassEntity classEntity, boolean mandatory) {
        Optional<User> existingUser = userRepository.findByUsername(item.getUsername().trim().toLowerCase());

        User student;
        boolean isNew = false;

        if (existingUser.isPresent()) {
            student = existingUser.get();
            boolean alreadyStudent = student.getRoles().stream().anyMatch(role -> role.getName() == RoleName.STUDENT);
            boolean hasRestrictedRole = student.getRoles().stream()
                    .anyMatch(role -> role.getName() == RoleName.TEACHER || role.getName() == RoleName.ADMIN);
            if (hasRestrictedRole && !alreadyStudent) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Username " + item.getUsername() + " đang thuộc tài khoản giáo viên hoặc quản trị, không thể import như học sinh");
            }
            if (item.getFullName() != null && !item.getFullName().isBlank()) {
                student.setFullName(item.getFullName().trim());
            }
            if (item.getEmail() != null && !item.getEmail().isBlank()) {
                student.setEmail(item.getEmail().trim().toLowerCase());
            }
            if (item.getStudentCode() != null) {
                student.setStudentCode(item.getStudentCode());
            }
            if (item.getPhone() != null) {
                student.setPhone(item.getPhone());
            }
            if (item.getAddress() != null) {
                student.setAddress(item.getAddress());
            }
            if (item.getGrade() != null) {
                student.setGrade(item.getGrade());
            }
            if (item.getFaculty() != null) {
                student.setFaculty(item.getFaculty());
            }
            student = userRepository.save(student);
        } else {
            isNew = true;
            String defaultEmail = item.getEmail() != null && !item.getEmail().isBlank()
                    ? item.getEmail().trim().toLowerCase()
                    : item.getUsername().trim().toLowerCase() + "@school.edu.vn";

            student = User.builder()
                    .username(item.getUsername().trim().toLowerCase())
                    .email(defaultEmail)
                    .password(passwordEncoder.encode(generateTemporaryPassword()))
                    .fullName(item.getFullName() != null ? item.getFullName().trim() : item.getUsername())
                    .studentCode(item.getStudentCode())
                    .phone(item.getPhone())
                    .address(item.getAddress())
                    .grade(item.getGrade())
                    .faculty(item.getFaculty())
                    .enabled(true)
                    .emailVerified(false)
                    .build();
            student.getRoles().add(requireStudentRole());
            student = userRepository.save(student);
        }

        if (student.getRoles().stream().noneMatch(role -> role.getName() == RoleName.STUDENT)) {
            student.getRoles().add(requireStudentRole());
            student = userRepository.save(student);
        }

        Optional<ClassStudent> existingEnrollment = classStudentRepository.findByClassEntityIdAndStudentId(
                classEntity.getId(), student.getId());

        if (existingEnrollment.isPresent()) {
            if (mandatory) {
                ClassStudent cs = existingEnrollment.get();
                cs.setJoinedAt(LocalDateTime.now());
                classStudentRepository.save(cs);
            }
        } else {
            ClassStudent classStudent = ClassStudent.builder()
                    .classEntity(classEntity)
                    .student(student)
                    .build();
            classStudentRepository.save(classStudent);
        }

        return StudentImportResult.builder()
                .username(item.getUsername())
                .success(true)
                .message(isNew ? "Tạo mới và thêm vào lớp" : "Cập nhật và thêm vào lớp")
                .studentId(student.getId())
                .isNew(isNew)
                .build();
    }

    public ClassEntity requireManageableClass(Long classId, User teacher) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học"));

        boolean isAdmin = teacher.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);

        if (!isAdmin && !classEntity.getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý lớp học này");
        }

        return classEntity;
    }

    private ClassResponse toClassResponse(ClassEntity classEntity) {
        long studentCount = classStudentRepository.countByClassEntityId(classEntity.getId());
        User teacher = classEntity.getTeacher();
        return ClassResponse.builder()
                .id(classEntity.getId())
                .name(classEntity.getName())
                .description(classEntity.getDescription())
                .subject(classEntity.getSubject())
                .classCode(classEntity.getClassCode())
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null ? teacher.getUsername() : "—")
                .studentCount(studentCount)
                .createdAt(toOffset(classEntity.getCreatedAt()))
                .updatedAt(toOffset(classEntity.getUpdatedAt()))
                .build();
    }

    private ClassStudentResponse toClassStudentResponse(ClassStudent classStudent) {
        return ClassStudentResponse.builder()
                .id(classStudent.getId())
                .classId(classStudent.getClassEntity().getId())
                .className(classStudent.getClassEntity().getName())
                .studentId(classStudent.getStudent().getId())
                .studentUsername(classStudent.getStudent().getUsername())
                .studentEmail(classStudent.getStudent().getEmail())
                .joinedAt(toOffset(classStudent.getJoinedAt()))
                .build();
    }

    private Role requireStudentRole() {
        return roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Role STUDENT chưa được cấu hình"));
    }

    private String generateTemporaryPassword() {
        return "Temp" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "1a";
    }

    private void syncExamClassName(String previousName, String newName, User teacher) {
        if (previousName == null || newName == null || previousName.equals(newName) || teacher == null) {
            return;
        }
        List<com.example.demo.domain.entity.Exam> exams = examRepository.findByCreatedBy(teacher);
        for (com.example.demo.domain.entity.Exam exam : exams) {
            if (previousName.equalsIgnoreCase(exam.getClassName())) {
                exam.setClassName(newName);
            }
        }
        examRepository.saveAll(exams);
    }
}
