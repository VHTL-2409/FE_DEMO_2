package com.example.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TeacherPageController {

    @GetMapping("/web/teacher")
    public String teacherPage() {
        return "teacher-dashboard";
    }
}
