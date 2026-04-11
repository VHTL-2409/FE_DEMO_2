package com.example.demo.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TeacherPageController {

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @GetMapping("/web/teacher")
    public String teacherPage() {
        String baseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return "redirect:" + baseUrl + "/teacher/dashboard";
    }
}
