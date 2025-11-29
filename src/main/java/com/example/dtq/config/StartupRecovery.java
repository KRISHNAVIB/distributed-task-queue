package com.example.dtq.config;

import com.example.dtq.model.JobStatus;
import com.example.dtq.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRecovery implements CommandLineRunner {

    private final JobRepository repo;

    public StartupRecovery(JobRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        var runningJobs = repo.getRunningJobs();

        if (runningJobs == null || runningJobs.isEmpty()) {
            return;
        }

        for (String jobId : runningJobs) {
            repo.updateJobStatus(jobId, JobStatus.PENDING);
            repo.removeFromRunningSet(jobId);
            repo.removeLease(jobId);
            repo.pushToPendingQueue(jobId);
        }
    }
}
