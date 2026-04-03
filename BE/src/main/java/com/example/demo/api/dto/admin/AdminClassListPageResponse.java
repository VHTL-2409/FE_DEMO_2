package com.example.demo.api.dto.admin;

import com.example.demo.api.dto.classmanagement.ClassResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClassListPageResponse {
    private List<ClassResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
