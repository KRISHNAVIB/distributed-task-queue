package com.example.dtq.controller;

import lombok.Data;

@Data
public class JobRequest {
    private String tenantId;
    private String payloadJson;
    private int maxRetries=3;
}
