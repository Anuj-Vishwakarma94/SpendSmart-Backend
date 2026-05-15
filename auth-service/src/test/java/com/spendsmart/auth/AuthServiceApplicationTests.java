package com.spendsmart.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Fails to load context without environment variables and database connection")
@SpringBootTest(classes = AuthServiceApplication.class)
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
