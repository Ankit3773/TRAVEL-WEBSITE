package com.travel.travelapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravelappApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelappApplication.class, args);
	}

}
