package com.example.demo.api.dto.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportIssueDto {
    private Long id;
    private String issueType;
    private String severity;
    private Integer questionIndex;
    private String issueData;
    private Boolean resolved;
}
