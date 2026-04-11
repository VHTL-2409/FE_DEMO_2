package com.example.demo.api.dto.classmanagement;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentImportItem {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    private String username;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    @Size(max = 50, message = "Mã sinh viên không được vượt quá 50 ký tự")
    private String studentCode;

    private String birthDate;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @Size(max = 200, message = "Địa chỉ không được vượt quá 200 ký tự")
    private String address;

    @Size(max = 50, message = "Khối/lớp không được vượt quá 50 ký tự")
    private String grade;

    @Size(max = 100, message = "Khoa không được vượt quá 100 ký tự")
    private String faculty;
}
