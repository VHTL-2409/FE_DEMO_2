package com.example.demo.api.dto.importer;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ImportFromAzotaRequest {
    @Valid
    private List<ImportFromAzotaQuestionDto> questions;
}
