package com.expensetracker;

import com.spendsmart.analytics.AnalyticsServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AnalyticsServiceApplication.class,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:testdb",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "jwt.secret=testSecretKey1234567890123456789012",
            "eureka.client.enabled=false",
            "eureka.client.register-with-eureka=false",
            "eureka.client.fetch-registry=false",
            "spring.boot.admin.client.enabled=false"
        })
class AnalyticsServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
