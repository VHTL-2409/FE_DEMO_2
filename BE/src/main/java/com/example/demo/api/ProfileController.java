package com.example.demo.api;

import com.example.demo.api.dto.profile.ProfileResponse;
import com.example.demo.api.dto.profile.ProfileUpdateRequest;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final ProfileService profileService;

    public ProfileController(CurrentUserService currentUserService, ProfileService profileService) {
        this.currentUserService = currentUserService;
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        User user = currentUserService.requireCurrentUser();
        ProfileResponse studentProfile = profileService.getStudentProfile(user);
        ProfileResponse teacherProfile = profileService.getTeacherProfile(user);
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "roles", user.getRoles().stream().map(r -> r.getName().name()).toList(),
            "studentProfile", studentProfile,
            "teacherProfile", teacherProfile
        );
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
