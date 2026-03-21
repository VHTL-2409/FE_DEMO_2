package com.example.demo.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExamListPageResponse {
    private List<AdminExamListItemResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
