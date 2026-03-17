package com.example.demo.api;

import com.example.demo.api.dto.auth.RoleAssignmentRequest;
import com.example.demo.api.dto.profile.ProfileResponse;
import com.example.demo.api.dto.profile.ProfileUpdateRequest;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ProfileService;
import com.example.demo.service.UserRoleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final CurrentUserService currentUserService;
    private final ProfileService profileService;
    private final UserRoleService userRoleService;

    public ProfileController(CurrentUserService currentUserService, ProfileService profileService, UserRoleService userRoleService) {
        this.currentUserService = currentUserService;
        this.profileService = profileService;
        this.userRoleService = userRoleService;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        User user = currentUserService.requireCurrentUser();
        var roles = (user.getRoles() != null ? user.getRoles() : java.util.Set.<com.example.demo.domain.entity.Role>of())
                .stream()
                .map(r -> r.getName().name())
                .toList();
        boolean isTeacher = roles.contains("TEACHER") || roles.contains("ROLE_TEACHER");
        boolean isStudent = roles.contains("STUDENT") || roles.contains("ROLE_STUDENT");
        ProfileResponse studentProfile = null;
        ProfileResponse teacherProfile = null;
        try {
            if (isStudent) studentProfile = profileService.getStudentProfile(user);
        } catch (Exception e) {
            log.warn("Failed to load student profile for user {}: {}", user.getUsername(), e.getMessage());
        }
        try {
            if (isTeacher) teacherProfile = profileService.getTeacherProfile(user);
        } catch (Exception e) {
            log.warn("Failed to load teacher profile for user {}: {}", user.getUsername(), e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername() != null ? user.getUsername() : "");
        result.put("email", user.getEmail() != null ? user.getEmail() : "");
        result.put("roles", roles);
        result.put("studentProfile", studentProfile);
        result.put("teacherProfile", teacherProfile);
        return result;
    }

    @GetMapping("/profile/student")
    public ProfileResponse studentProfile() {
        User user = currentUserService.requireCurrentUser();
        return profileService.getStudentProfile(user);
    }

    @GetMapping("/profile/teacher")
    public ProfileResponse teacherProfile() {
        User user = currentUserService.requireCurrentUser();
        return profileService.getTeacherProfile(user);
    }

    @PostMapping("/me/role")
    public Map<String, Object> assignRole(@RequestBody(required = false) RoleAssignmentRequest request) {
        if (request == null || request.getRole() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        User user = currentUserService.requireCurrentUser();
        return Map.of("roles", userRoleService.assignRole(user, request.getRole()));
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody ProfileUpdateRequest request) {
        User user = currentUserService.requireCurrentUser();
        profileService.updateSharedProfile(user, request);
        return Map.of("status", "ok");
    }

    @PostMapping("/profile/avatar")
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh đại diện.");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/jpg")
                && !contentType.equalsIgnoreCase("image/png"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ ảnh JPG hoặc PNG.");
        }

        long maxSize = 2L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ảnh vượt quá giới hạn 2MB.");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }

        String filename = "avatar-" + UUID.randomUUID() + extension;
        Path uploadDir = Paths.get("src/main/resources/static/avatars");
        try {
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh đại diện.");
        }

        String avatarUrl = "/avatars/" + filename;
        User user = currentUserService.requireCurrentUser();
        profileService.updateAvatar(user, avatarUrl);

        return Map.of("avatarUrl", avatarUrl);
    }
}
