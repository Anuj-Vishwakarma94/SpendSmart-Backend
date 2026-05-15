package com.spendsmart.auth.config;

import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Default admin credentials ───────────────────────────
    @org.springframework.beans.factory.annotation.Value("${admin.default.email:anujvishwakarma9827@gmail.com}")
    private String adminEmail;

    @org.springframework.beans.factory.annotation.Value("${admin.default.password:#{null}}")
    private String adminPassword;

    @org.springframework.beans.factory.annotation.Value("${admin.default.name:Anuj Vishwakarma}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("⚠️ Admin password not configured. Skipping admin initialization.");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("✅ Admin account already exists: {}", adminEmail);
            return;
        }

        User admin = User.builder()
                .fullName(adminName)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .currency("INR")
                .timezone("Asia/Kolkata")
                .provider(User.AuthProvider.LOCAL)
                .isActive(true)
                .role(User.Role.ADMIN)
                .build();

        userRepository.save(admin);

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🛡️  Admin account created automatically");
        log.info("   Email   : {}", adminEmail);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
