package com.jaeuk.job_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class JobAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobAiApplication.class, args);
	}

}
