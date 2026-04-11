package com.example.demo.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageDto {

    @NotBlank
    @Size(max = 16)
    private String role;

    @NotBlank
    @Size(max = 12000)
    private String content;
}
