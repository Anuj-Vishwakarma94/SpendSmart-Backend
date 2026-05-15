package com.spendsmart.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDiscoveryClient
public class BudgetServiceApplication {
    public static void main(String[] args) { SpringApplication.run(BudgetServiceApplication.class, args); }
}
