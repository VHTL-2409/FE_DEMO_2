package com.example.demo.api;

import com.example.demo.api.dto.profile.ProfileResponse;
import com.example.demo.api.dto.profile.ProfileUpdateRequest;
import com.example.demo.domain.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
}
