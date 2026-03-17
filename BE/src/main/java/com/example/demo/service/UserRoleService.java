package com.example.demo.service;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Role;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProfileService profileService;

    @Transactional
    public List<String> assignRole(User user, String roleName) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "Role already assigned");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        RoleName normalized;
        try {
            normalized = RoleName.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        if (normalized != RoleName.TEACHER && normalized != RoleName.STUDENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role not allowed");
        }

        Role role = roleRepository.findByName(normalized)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Role not found"));

        user.getRoles().clear();
        user.getRoles().add(role);
        userRepository.save(user);

        if (normalized == RoleName.TEACHER) {
            profileService.removeStudentProfile(user);
        } else if (normalized == RoleName.STUDENT) {
            profileService.removeTeacherProfile(user);
        }

        return List.of(role.getName().name());
    }
}
