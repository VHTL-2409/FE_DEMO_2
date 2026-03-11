package com.example.demo.api.dto.submission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitAttemptRequest {
    @Valid
    @NotEmpty
    private List<AnswerInput> answers;
}
