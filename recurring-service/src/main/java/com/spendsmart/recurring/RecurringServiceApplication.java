package com.spendsmart.recurring;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class RecurringServiceApplication {
    public static void main(String[] args) { SpringApplication.run(RecurringServiceApplication.class, args); }
}
