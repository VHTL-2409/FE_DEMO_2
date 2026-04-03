package com.example.demo.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống.")
    @Size(min = 3, max = 100, message = "Tên đăng nhập phải từ 3 đến 100 ký tự.")
    private String username;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Định dạng email không hợp lệ. Vui lòng nhập email đúng định dạng (ví dụ: name@example.com).")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
             message = "Email phải có định dạng hợp lệ (ví dụ: name@example.com).")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số.")
    private String password;
}
