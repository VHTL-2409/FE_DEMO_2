package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AttemptFilterResponse {
    private List<AttemptSummaryResponse> items;
    private long total;
    private int page;
    private int size;
}
