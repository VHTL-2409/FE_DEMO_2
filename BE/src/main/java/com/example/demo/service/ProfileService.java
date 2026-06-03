package com.example.demo.service;

import com.example.demo.api.dto.profile.ProfileResponse;
import com.example.demo.api.dto.profile.ProfileUpdateRequest;
import com.example.demo.domain.entity.StudentProfile;
import com.example.demo.domain.entity.TeacherProfile;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.repository.TeacherProfileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;

    public ProfileService(StudentProfileRepository studentProfileRepository,
                          TeacherProfileRepository teacherProfileRepository,
                          UserRepository userRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createProfilesForUser(User user) {
        StudentProfile studentProfile = StudentProfile.builder()
                .user(user)
                .build();
        TeacherProfile teacherProfile = TeacherProfile.builder()
                .user(user)
                .build();

        studentProfileRepository.save(studentProfile);
        teacherProfileRepository.save(teacherProfile);
    }

    @Transactional
    public void removeStudentProfile(User user) {
        studentProfileRepository.findByUser(user).ifPresent(studentProfileRepository::delete);
    }

    @Transactional
    public void removeTeacherProfile(User user) {
        teacherProfileRepository.findByUser(user).ifPresent(teacherProfileRepository::delete);
    }

    @Transactional
    public ProfileResponse getStudentProfile(User user) {
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseGet(() -> studentProfileRepository.save(StudentProfile.builder().user(user).build()));
        return toResponse(profile, user);
    }

    @Transactional
    public ProfileResponse getTeacherProfile(User user) {
        TeacherProfile profile = teacherProfileRepository.findByUser(user)
                .orElseGet(() -> teacherProfileRepository.save(TeacherProfile.builder().user(user).build()));
        return toResponse(profile, user);
    }

    @Transactional
    public void updateSharedProfile(User user, ProfileUpdateRequest request) {
        boolean updated = false;

        if (request.getDisplayName() != null) {
            String displayName = request.getDisplayName().trim();
            if (!displayName.isEmpty()) {
                user.setUsername(displayName);
                updated = true;
            }
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim());
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
        }

        studentProfileRepository.findByUser(user).ifPresent(profile -> {
            applyProfileUpdate(profile, request);
            studentProfileRepository.save(profile);
        });

        teacherProfileRepository.findByUser(user).ifPresent(profile -> {
            applyProfileUpdate(profile, request);
            teacherProfileRepository.save(profile);
        });
    }

    @Transactional
    public void updateAvatar(User user, String avatarUrl) {
        studentProfileRepository.findByUser(user).ifPresent(profile -> {
            profile.setAvatarUrl(avatarUrl);
            studentProfileRepository.save(profile);
        });

        teacherProfileRepository.findByUser(user).ifPresent(profile -> {
            profile.setAvatarUrl(avatarUrl);
            teacherProfileRepository.save(profile);
        });
    }

    private void applyProfileUpdate(StudentProfile profile, ProfileUpdateRequest request) {
        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName().trim());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getCitizenId() != null) {
            profile.setCitizenId(request.getCitizenId().trim());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl().trim());
        }
    }

    private void applyProfileUpdate(TeacherProfile profile, ProfileUpdateRequest request) {
        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName().trim());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl().trim());
        }
    }

    private ProfileResponse toResponse(StudentProfile profile, User user) {
        return new ProfileResponse(
                profile.getId(),
                user.getId(),
                user.getUsername(),
                profile.getDisplayName(),
                profile.getFullName(),
                profile.getDateOfBirth(),
                profile.getCitizenId(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getAvatarUrl());
    }

    private ProfileResponse toResponse(TeacherProfile profile, User user) {
        return new ProfileResponse(
                profile.getId(),
                user.getId(),
                user.getUsername(),
                profile.getDisplayName(),
                profile.getFullName(),
                profile.getDateOfBirth(),
                null,
                profile.getEmail(),
                profile.getPhone(),
                profile.getAvatarUrl());
    }
}
