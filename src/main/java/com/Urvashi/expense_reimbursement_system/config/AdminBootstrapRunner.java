package com.Urvashi.expense_reimbursement_system.config;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditService;
import com.Urvashi.expense_reimbursement_system.user.Role;
import com.Urvashi.expense_reimbursement_system.user.User;
import com.Urvashi.expense_reimbursement_system.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public AdminBootstrapRunner(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                AuditService auditService,
                                @Value("${app.bootstrap.admin.email:}") String adminEmail,
                                @Value("${app.bootstrap.admin.password:}") String adminPassword,
                                @Value("${app.bootstrap.admin.full-name:System Admin}") String adminFullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("Admin bootstrap skipped: app.bootstrap.admin.email / password not set");
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            log.info("Admin bootstrap skipped: a user with the configured email already exists");
            return;
        }
        if (adminPassword.length() < 8) {
            throw new IllegalStateException("app.bootstrap.admin.password must be at least 8 characters");
        }

        User admin = new User();
        admin.setFullName(adminFullName.trim());
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        User saved = userRepository.save(admin);

        auditService.record(null, AuditAction.USER_REGISTERED, "USER", saved.getId(),
                "Bootstrap ADMIN account created at startup");
        log.info("Bootstrap admin account created for {}", email);
    }
}