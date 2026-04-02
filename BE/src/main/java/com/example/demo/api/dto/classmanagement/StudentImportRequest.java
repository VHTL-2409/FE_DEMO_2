package com.example.demo.api.dto.classmanagement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StudentImportRequest {

    @NotEmpty(message = "Danh sách học sinh không được để trống")
    @Valid
    private List<StudentImportItem> students;

    private boolean mandatory = true;
}
