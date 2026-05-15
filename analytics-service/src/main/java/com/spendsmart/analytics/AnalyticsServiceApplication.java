package com.spendsmart.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDiscoveryClient
public class AnalyticsServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AnalyticsServiceApplication.class, args); }
}
