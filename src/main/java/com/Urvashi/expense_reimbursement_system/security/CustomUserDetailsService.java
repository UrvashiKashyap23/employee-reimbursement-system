package com.Urvashi.expense_reimbursement_system.security;

import com.Urvashi.expense_reimbursement_system.user.User;
import com.Urvashi.expense_reimbursement_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Spring's own User class has the same name as ours, so we write its full name
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())      // becomes authority "ROLE_EMPLOYEE", etc.
                .disabled(!user.isActive())        // deactivated users cannot log in
                .build();
    }
}