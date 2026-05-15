package com.spendsmart.income;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDiscoveryClient
public class IncomeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IncomeServiceApplication.class, args);
    }
}
