package com.example.dtq.repository;

import com.example.dtq.model.Job;
import com.example.dtq.model.JobStatus;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

@Repository
public class JobRepository {

    private final RedisTemplate<String,String> redis;
    public JobRepository(RedisTemplate<String,String> redisTemplate){
        this.redis=redisTemplate;
    }

    private String jobKey(String jobId)
    {
        return jobId;
    }

    public void saveJob(Job job)
    {
        String key=job.getId();

        redis.opsForHash().put(key,"tenantId",job.getTenantId());
        redis.opsForHash().put(key,"status",job.getStatus().name());
        redis.opsForHash().put(key,"payloadJson",job.getPayloadJson());
        redis.opsForHash().put(key,"retries",String.valueOf(job.getRetries()));
        redis.opsForHash().put(key,"maxRetries",String.valueOf(job.getMaxRetries()));
        redis.opsForHash().put(key,"createdAt",job.getCreatedAt().toString());
        redis.opsForHash().put(key,"updatedAt",job.getUpdatedAt().toString());
    }

    public void pushToPendingQueue(String jobId){
        redis.opsForList().leftPush("queue:pending",jobId);
    }

    public String popNextPendingJob(){
        return redis.opsForList().rightPop("queue:pending");
    }

    public void updateJobStatus(String jobId, JobStatus status){
        redis.opsForHash().put(jobId,"status",status.name());
    }

    public int incrementRetries(String jobId) {
        Long newValue=redis.opsForHash().increment(jobId,"retries",1);
        return newValue!=null? newValue.intValue() : 0;
    }

    public void moveToDLQ(String jobId) {
        redis.opsForSet().add("queue:dlq",jobId);
        redis.opsForHash().put(jobId,"status",JobStatus.DLQ.name());
        redis.opsForHash().put(jobId, "updatedAt", java.time.Instant.now().toString());
    }

    public void markJobRunning(String jobId) {
        redis.opsForSet().add("queue:running",jobId);
        redis.opsForHash().put(jobId,"status",JobStatus.RUNNING.name());

        Object tenantObj=redis.opsForHash().get(jobId,"tenantId");
        if(tenantObj!=null)
        {
            String tenantId=tenantObj.toString();
            String activeKey="tenant:"+ tenantId +":active_jobs";

            redis.opsForValue().increment(activeKey);
        }
    }

    //SET NX EX <second>
    public boolean acquireLease(String jobId,String workerId,Long leaseSecond)
    {
        String leaseKey=jobId+":lease";

        Boolean success=redis.opsForValue().setIfAbsent(
                leaseKey,
                workerId,
                java.time.Duration.ofSeconds(leaseSecond)
        );
        return Boolean.TRUE.equals(success);
    }

    public void removeLease(String jobId){
        String leaseKey=jobId+":lease";
        redis.delete(leaseKey);
    }

    public void addToRunningSet(String jobId)
    {
        redis.opsForSet().add("queue:running",jobId);
    }

    public void removeFromRunningSet(String jobId)
    {
        redis.opsForSet().remove("queue:running",jobId);

        Object tenantObj=redis.opsForHash().get(jobId,"tenantId");
        if(tenantObj!=null)
        {
            String tenantId=tenantObj.toString();
            String tenantActiveKey="tenant:"+ tenantId +":active_jobs";
            redis.opsForValue().decrement(tenantActiveKey);
        }
    }

    public void markJobCompleted(String jobId){
        redis.opsForHash().put(jobId,"status",JobStatus.COMPLETED.name());
        redis.opsForHash().put(jobId,"updatedAt", java.time.Instant.now().toString());
    }

    public Object getJobField(String jobId, String field){
        return redis.opsForHash().get(jobId,field);
    }

    public void saveLastError(String jobId,String errorMessage){
        redis.opsForHash().put(jobId,"lastError",errorMessage);
    }

    public Long incrementCounterWithExpiry(String key,long expirySeconds){
        Long count=redis.opsForValue().increment(key);
        if(count == 1){
            redis.expire(key, Duration.ofSeconds(expirySeconds));
        }        return count;
    }

    public Long getOrDefault(String key,long defaultVal){
        String value=redis.opsForValue().get(key);
        return value!=null?Long.parseLong(value):defaultVal;
    }

    public Long incrementCounter(String key){
        return redis.opsForValue().increment(key);
    }

    public Map<Object, Object> getAllJobFields(String jobId)
    {
        return redis.opsForHash().entries(jobId);
    }

    public List<String> getPendingJobs(){
        return redis.opsForList().range("queue:pending",0,-1);
    }

    public Set<String> getRunningJobs(){
        return redis.opsForSet().members("queue:running");
    }

    public Set<String> getDLQJobs(){
        return redis.opsForSet().members("queue:dlq");
    }

    public List<String> getCompletedJobs() {
        Set<String> keys = redis.keys("job:*");
        List<String> completed = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {

                DataType type = redis.type(key);
                if (!DataType.HASH.equals(type)) {
                    continue;
                }
                Object status = redis.opsForHash().get(key, "status");
                if (status != null && status.toString().equals("COMPLETED")) {
                    completed.add(key);
                }
            }
        }
        return completed;
    }

}
