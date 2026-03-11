package com.example.demo.api;

import com.example.demo.domain.entity.User;
import com.example.demo.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final CurrentUserService currentUserService;

    public ProfileController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        User user = currentUserService.requireCurrentUser();
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "roles", user.getRoles().stream().map(r -> r.getName().name()).toList()
        );
    }
}
