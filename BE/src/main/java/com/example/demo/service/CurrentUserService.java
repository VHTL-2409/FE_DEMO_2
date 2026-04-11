package com.example.demo.service;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByUsernameWithRoles(authentication.getName())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    public boolean isTeacherOrAdmin(User user) {
        return hasRole(user, RoleName.TEACHER) || hasRole(user, RoleName.ADMIN);
    }

    public void requireTeacherOrAdmin(User user) {
        if (!isTeacherOrAdmin(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Teacher or admin access required");
        }
    }

    public boolean isStudentOrAdmin(User user) {
        return hasRole(user, RoleName.STUDENT) || hasRole(user, RoleName.ADMIN);
    }

    public void requireStudentOrAdmin(User user) {
        if (!isStudentOrAdmin(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student or admin access required");
        }
    }

    public boolean isAdmin(User user) {
        return hasRole(user, RoleName.ADMIN);
    }

    public void requireAdmin(User user) {
        if (!isAdmin(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
