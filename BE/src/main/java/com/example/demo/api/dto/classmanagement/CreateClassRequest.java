package com.example.demo.api.dto.classmanagement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassRequest {
    @NotBlank(message = "Tên lớp không được để trống")
    @Size(max = 100, message = "Tên lớp không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @Size(max = 100, message = "Môn học không được vượt quá 100 ký tự")
    private String subject;
}
