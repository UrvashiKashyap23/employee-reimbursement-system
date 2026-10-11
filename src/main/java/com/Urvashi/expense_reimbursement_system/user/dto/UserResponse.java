package com.Urvashi.expense_reimbursement_system.user.dto;

import com.Urvashi.expense_reimbursement_system.user.Role;
import com.Urvashi.expense_reimbursement_system.user.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        Long managerId,
        String managerName,
        boolean active,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        User manager = user.getManager();
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFullName() : null,
                user.isActive(),
                user.getCreatedAt()
        );
    }
}