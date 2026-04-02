package com.example.demo.api.dto.classmanagement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinClassByCodeRequest {

    @NotBlank(message = "Mã lớp không được để trống")
    @Size(min = 6, max = 6, message = "Mã lớp phải có 6 ký tự")
    private String classCode;
}
