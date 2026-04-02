package com.example.demo.api.dto.classmanagement;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddStudentsRequest {
    @NotNull(message = "Danh sách học sinh không được để trống")
    private java.util.List<Long> studentIds;
}
