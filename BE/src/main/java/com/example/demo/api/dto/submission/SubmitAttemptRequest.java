package com.example.demo.api.dto.submission;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitAttemptRequest {
    @Valid
    private List<AnswerInput> answers;
}
