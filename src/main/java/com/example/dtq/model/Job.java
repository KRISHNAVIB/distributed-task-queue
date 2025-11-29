package com.example.dtq.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class Job {

    private final String id;
    private String tenantId;
    private JobStatus status;
    private String payloadJson;
    private int retries;
    private int maxRetries;
    private Instant createdAt;
    private Instant updatedAt;
    private String lastError;

    public Job(String tenantId, String payloadJson, int maxRetries)
    {
        this.id="job:" + UUID.randomUUID().toString();
        this.tenantId=tenantId;
        this.payloadJson = payloadJson;
        this.status=JobStatus.PENDING;
        this.retries=0;
        this.maxRetries=maxRetries;
        this.createdAt=Instant.now();
        this.updatedAt=this.createdAt;
    }

}
