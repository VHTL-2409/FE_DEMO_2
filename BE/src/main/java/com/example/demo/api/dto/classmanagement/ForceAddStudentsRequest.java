package com.example.demo.api.dto.classmanagement;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ForceAddStudentsRequest {

    @NotEmpty(message = "Danh sách học sinh không được để trống")
    private List<Long> studentIds;

    private boolean mandatory = true;
}
