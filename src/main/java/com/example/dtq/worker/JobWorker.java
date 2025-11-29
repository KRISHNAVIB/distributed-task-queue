package com.example.dtq.worker;

import com.example.dtq.model.JobStatus;
import com.example.dtq.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobWorker {

    private final JobRepository repo;

    public JobWorker(JobRepository repo) {
        this.repo = repo;
    }
    @Async
    @Scheduled(fixedDelay = 1000)
    public void pollAndProcess(){
        String jobId=repo.popNextPendingJob();

        if(jobId==null)
        {
            log.info("No Jobs in queue");
            return;
        }
        log.info("Picked Job from queue: {}",jobId);

        boolean leaseAcquired=repo.acquireLease(jobId,"worker-1",30L);
        if(!leaseAcquired){
            log.info("Could not acquire lease for Job: {} - another worker is processing",jobId);
            return;
        }
        log.info("Lease acquired for job: {}",jobId);

        Object tenantObj=repo.getJobField(jobId,"tenantId");
        if(tenantObj!=null){
            String tenantId=tenantObj.toString();
            String activeKey="tenant:" + tenantId + ":active_jobs";

            Long active=repo.getOrDefault(activeKey,0L);

            if(active>=5){
                log.warn("Tenant {} reached max concurrency. Requeue job {}",tenantId,jobId);
                repo.updateJobStatus(jobId, JobStatus.PENDING);
                repo.pushToPendingQueue(jobId);
                repo.removeLease(jobId);
                return;
            }
        }

        repo.markJobRunning(jobId);
        log.info("Marked job as RUNNING: {}",jobId);

        try{
            log.info("Processing job: {}",jobId);
            Object payloadObj = repo.getJobField(jobId, "payloadJson");
            String payload = payloadObj != null ? payloadObj.toString() : "{}";
            boolean forceFail = payload.contains("\"forceFail\":true");

            Thread.sleep(15000);
            if(forceFail){
                throw new RuntimeException("Simulated Failure");
            }
            boolean success = true;

            if (success) {
                log.info("Job Processed successfully: {}", jobId);
                handleSuccess(jobId);
            } else {
                throw new RuntimeException("Simulated Failure");
            }
        } catch (Exception e) {
            log.error("Error while processing job{} - {}",jobId,e.getMessage());
            handleFailure(jobId,e.getMessage());
        }

    }
    private void handleSuccess(String jobId)
    {
        repo.markJobCompleted(jobId);
        repo.saveLastError(jobId, "");
        repo.removeFromRunningSet(jobId);
        repo.removeLease(jobId);
        repo.incrementCounter("counter:jobs_completed");
        log.info("Job COMPLETED successfully: {}",jobId);
    }

    private void handleFailure(String jobId,String errorMessage){
        int retries=repo.incrementRetries(jobId);

        Object maxRetryObj=repo.getJobField(jobId,"maxRetries");
        int maxRetries=maxRetryObj!=null?Integer.parseInt(maxRetryObj.toString()):3;
        repo.saveLastError(jobId,errorMessage);

        if(retries<=maxRetries){
            repo.updateJobStatus(jobId, JobStatus.PENDING);
            repo.pushToPendingQueue(jobId);
            repo.incrementCounter("counter:jobs_retried");
            log.warn("Retrying job {} (attempt {}/{})",jobId,retries,maxRetries);
        }
        else{
            repo.moveToDLQ(jobId);
            repo.incrementCounter("counter:jobs_failed");
            log.error("job {} moved to DLQ after {} retries",jobId,retries);
        }
        repo.removeFromRunningSet(jobId);
        repo.removeLease(jobId);
    }
}
