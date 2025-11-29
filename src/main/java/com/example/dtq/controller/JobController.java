package com.example.dtq.controller;

import com.example.dtq.repository.JobRepository;
import com.example.dtq.service.IdempotencyService;
import com.example.dtq.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final IdempotencyService idempotencyService;
    private final JobRepository repo;

    public JobController(JobService jobService, IdempotencyService idempotencyService, JobRepository repo){
        this.jobService=jobService;
        this.idempotencyService = idempotencyService;
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<?> submitJob(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JobRequest request) {
        String tenantId=request.getTenantId();
        if(idempotencyKey!=null && !idempotencyKey.isBlank()){
            String existingJobId=idempotencyService.getJobIdForKey(tenantId,idempotencyKey);
            if(existingJobId!=null){
                Map<String, String> resp = new HashMap<>();
                resp.put("jobId", existingJobId);
                return ResponseEntity.ok(resp);            }
        }
        ResponseEntity<?> response=jobService.submitJob(request);
        String createdJobId = response.getBody().toString();
        if(idempotencyKey!=null && !idempotencyKey.isBlank() && response.getStatusCode().is2xxSuccessful()){
            idempotencyService.saveJobIdIfAbsent(tenantId,idempotencyKey,createdJobId);
        }
        Map<String, String> resp = new HashMap<>();
        resp.put("jobId", createdJobId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId){
        return jobService.getJobStatus(jobId);
    }

    @GetMapping("/pending")
    public List<String> getPending() {
        return jobService.getPendingJobs();
    }

    @GetMapping("/running")
    public Set<String> getRunning() {
        return jobService.getRunningJobs();
    }

    @GetMapping("/completed")
    public List<String> getCompleted() {
        return jobService.getCompletedJobs();
    }

    @GetMapping("/dlq")
    public Set<String> getDLQ() {
        return jobService.getDLQJobs();
    }


    @GetMapping("/test")
    public String test() {
        return "controller working";
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> metrics(){
        Map<String,Long> m=new HashMap<>();
        m.put("submitted", repo.getOrDefault("counter:jobs_submitted", 0L));
        m.put("completed", repo.getOrDefault("counter:jobs_completed", 0L));
        m.put("failed", repo.getOrDefault("counter:jobs_failed", 0L));
        m.put("retried", repo.getOrDefault("counter:jobs_retried", 0L));
        return ResponseEntity.ok(m);
    }

}
