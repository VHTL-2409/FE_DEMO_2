package com.example.demo.api.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class ChatResponse {

    private String status;
    private String reply;
    private String model;
    private Map<String, Object> usage;
    private String error;
}
