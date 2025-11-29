package com.example.dtq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class DistributedTaskQueueApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributedTaskQueueApplication.class, args);
	}

}
