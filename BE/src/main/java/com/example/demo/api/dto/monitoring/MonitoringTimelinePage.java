package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringTimelinePage {
    private java.util.List<MonitoringTimelineItem> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
