package com.Urvashi.expense_reimbursement_system.auth;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditService;
import com.Urvashi.expense_reimbursement_system.auth.dto.AuthResponse;
import com.Urvashi.expense_reimbursement_system.auth.dto.LoginRequest;
import com.Urvashi.expense_reimbursement_system.auth.dto.RegisterRequest;
import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.common.exception.ResourceNotFoundException;
import com.Urvashi.expense_reimbursement_system.security.JwtService;
import com.Urvashi.expense_reimbursement_system.user.Role;
import com.Urvashi.expense_reimbursement_system.user.User;
import com.Urvashi.expense_reimbursement_system.user.UserRepository;
import com.Urvashi.expense_reimbursement_system.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("An account with this email already exists", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));   // never store the raw password
        user.setRole(Role.EMPLOYEE);                                        // always EMPLOYEE for public sign-up
        user.setActive(true);
        User saved = userRepository.save(user);

        auditService.record(saved, AuditAction.USER_REGISTERED, "USER", saved.getId(),
                "Self-registration as EMPLOYEE");

        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());

        // Checks the password against the stored hash. Throws an AuthenticationException on failure,
        // which GlobalExceptionHandler turns into a 401 "Invalid email or password".
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", jwtService.getExpiresInSeconds(), UserResponse.from(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}