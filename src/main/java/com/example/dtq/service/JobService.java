package com.example.dtq.service;

import com.example.dtq.controller.JobRequest;
import com.example.dtq.model.Job;
import com.example.dtq.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class JobService {
    private final JobRepository repo;

    public JobService(JobRepository repo)
    {
        this.repo = repo;
    }

    public ResponseEntity<?> submitJob(JobRequest request){
        String tenantId=request.getTenantId();
        log.info("Received job submission for tenant={} payload={}", tenantId, request.getPayloadJson());
        if(tenantId==null|tenantId.isEmpty()){
            return ResponseEntity.badRequest().body("tenantId is Required");
        }

        String rateKey=rateKey(tenantId);
        Long rateCount=repo.incrementCounterWithExpiry(rateKey,60);

        if(rateCount>10) {
            log.warn("Rate limit exceeded for tenant={}, count={}", tenantId, rateCount);
            return ResponseEntity.status(429).body("Rate limit exceeded(10jobs/min)");
        }

        Job job =new Job(tenantId, request.getPayloadJson(), request.getMaxRetries());
        repo.saveJob(job);
        log.info("Job {} queued successfully for tenant={}", job.getId(), tenantId);
        repo.pushToPendingQueue(job.getId());
        repo.incrementCounter("counter:jobs_submitted");
        return ResponseEntity.ok(job.getId());
    }

    public ResponseEntity<?> getJobStatus(String jobId) {
        var map=repo.getAllJobFields(jobId);
        if(map==null || map.isEmpty()) {
            return ResponseEntity.status(404).body("Job not found");
        }
        return ResponseEntity.ok().body(map);
    }

    private String rateKey(String tenantId){
        return "tenant:" + tenantId +":rate";
    }


    public List<String> getPendingJobs(){
        return repo.getPendingJobs();
    }

    public Set<String> getRunningJobs(){
        return repo.getRunningJobs();
    }

    public List<String> getCompletedJobs(){
        return repo.getCompletedJobs();
    }

    public Set<String> getDLQJobs(){
        return repo.getDLQJobs();
    }
}
