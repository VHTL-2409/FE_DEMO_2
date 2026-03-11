package com.example.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StudentPageController {

    @GetMapping("/web/student")
    public String studentPage() {
        return "student-exam";
    }
}
