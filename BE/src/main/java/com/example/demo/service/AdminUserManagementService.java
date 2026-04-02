package com.example.demo.service;

import com.example.demo.api.dto.admin.AdminUserDetailResponse;
import com.example.demo.api.dto.admin.AdminUserListItemResponse;
import com.example.demo.api.dto.admin.AdminUserListPageResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.StudentProfile;
import com.example.demo.domain.entity.TeacherProfile;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.repository.TeacherProfileRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    @Transactional(readOnly = true)
    public AdminUserListPageResponse listStudents(int page, int size, String q) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size), Sort.by("username").ascending());
        Page<User> userPage = resolveStudentPage(q, pageable);
        return mapPage(userPage, this::loadStudentProfiles, this::fromStudent);
    }

    @Transactional(readOnly = true)
    public AdminUserListPageResponse listTeachers(int page, int size, String q) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size), Sort.by("username").ascending());
        Page<User> userPage = resolveTeacherPage(q, pageable);
        return mapPage(userPage, this::loadTeacherProfiles, this::fromTeacher);
    }

    @Transactional(readOnly = true)
    public AdminUserListPageResponse listAdmins(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size), Sort.by("username").ascending());
        Page<User> userPage = userRepository.findByRoleName(RoleName.ADMIN, pageable);
        return mapPage(userPage, ids -> Map.of(), (u, p) -> fromUserBasic(u));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getStudentDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        if (!hasRole(user, RoleName.STUDENT)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản không phải học sinh");
        }
        StudentProfile p = studentProfileRepository.findByUser(user).orElse(null);
        return toStudentDetail(user, p);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getTeacherDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        if (!hasRole(user, RoleName.TEACHER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản không phải giáo viên");
        }
        TeacherProfile p = teacherProfileRepository.findByUser(user).orElse(null);
        return toTeacherDetail(user, p);
    }

    private Page<User> resolveStudentPage(String q, Pageable pageable) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.isEmpty()) {
            return userRepository.findByRoleName(RoleName.STUDENT, pageable);
        }
        String pat = "%" + escapeLike(trimmed.toLowerCase()) + "%";
        return userRepository.searchStudents(RoleName.STUDENT, pat, pageable);
    }

    private Page<User> resolveTeacherPage(String q, Pageable pageable) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.isEmpty()) {
            return userRepository.findByRoleName(RoleName.TEACHER, pageable);
        }
        String pat = "%" + escapeLike(trimmed.toLowerCase()) + "%";
        return userRepository.searchTeachers(RoleName.TEACHER, pat, pageable);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == roleName);
    }

    private AdminUserDetailResponse toStudentDetail(User u, StudentProfile p) {
        return AdminUserDetailResponse.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .emailVerified(u.getEmailVerified())
                .displayName(p != null ? p.getDisplayName() : null)
                .fullName(p != null ? p.getFullName() : null)
                .phone(p != null ? p.getPhone() : null)
                .dateOfBirth(p != null ? p.getDateOfBirth() : null)
                .avatarUrl(p != null ? p.getAvatarUrl() : null)
                .build();
    }

    private AdminUserDetailResponse toTeacherDetail(User u, TeacherProfile p) {
        return AdminUserDetailResponse.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .emailVerified(u.getEmailVerified())
                .displayName(p != null ? p.getDisplayName() : null)
                .fullName(p != null ? p.getFullName() : null)
                .phone(p != null ? p.getPhone() : null)
                .dateOfBirth(p != null ? p.getDateOfBirth() : null)
                .avatarUrl(p != null ? p.getAvatarUrl() : null)
                .build();
    }

    private int clampSize(int size) {
        if (size < 1) return 20;
        return Math.min(size, 100);
    }

    private <P> AdminUserListPageResponse mapPage(
            Page<User> userPage,
            java.util.function.Function<List<Long>, Map<Long, P>> loadProfiles,
            java.util.function.BiFunction<User, P, AdminUserListItemResponse> toItem
    ) {
        List<User> users = userPage.getContent();
        List<Long> ids = users.stream().map(User::getId).toList();
        Map<Long, P> profileByUserId = ids.isEmpty() ? Map.of() : loadProfiles.apply(ids);

        List<AdminUserListItemResponse> content = users.stream()
                .map(u -> toItem.apply(u, profileByUserId.get(u.getId())))
                .collect(Collectors.toList());

        return AdminUserListPageResponse.builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    private Map<Long, StudentProfile> loadStudentProfiles(List<Long> userIds) {
        List<StudentProfile> list = studentProfileRepository.findAllByUserIdInWithUser(userIds);
        Map<Long, StudentProfile> map = new HashMap<>();
        for (StudentProfile sp : list) {
            map.put(sp.getUser().getId(), sp);
        }
        return map;
    }

    private Map<Long, TeacherProfile> loadTeacherProfiles(List<Long> userIds) {
        List<TeacherProfile> list = teacherProfileRepository.findAllByUserIdInWithUser(userIds);
        Map<Long, TeacherProfile> map = new HashMap<>();
        for (TeacherProfile tp : list) {
            map.put(tp.getUser().getId(), tp);
        }
        return map;
    }

    private AdminUserListItemResponse fromStudent(User u, StudentProfile p) {
        return AdminUserListItemResponse.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .emailVerified(u.getEmailVerified())
                .displayName(p != null ? p.getDisplayName() : null)
                .fullName(p != null ? p.getFullName() : null)
                .phone(p != null ? p.getPhone() : null)
                .dateOfBirth(p != null ? p.getDateOfBirth() : null)
                .build();
    }

    private AdminUserListItemResponse fromTeacher(User u, TeacherProfile p) {
        return AdminUserListItemResponse.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .emailVerified(u.getEmailVerified())
                .displayName(p != null ? p.getDisplayName() : null)
                .fullName(p != null ? p.getFullName() : null)
                .phone(p != null ? p.getPhone() : null)
                .dateOfBirth(p != null ? p.getDateOfBirth() : null)
                .build();
    }

    private AdminUserListItemResponse fromUserBasic(User u) {
        return AdminUserListItemResponse.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .emailVerified(u.getEmailVerified())
                .displayName(null)
                .fullName(null)
                .phone(null)
                .dateOfBirth(null)
                .build();
    }
}
