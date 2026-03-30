package com.example.demo.api.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {

    @NotEmpty
    @Size(max = 40)
    @Valid
    private List<ChatMessageDto> messages;

    @Size(max = 128)
    private String model;
}
