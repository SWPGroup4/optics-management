package com.glassystem.optics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpticsManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpticsManagementApplication.class, args);
	}

}
