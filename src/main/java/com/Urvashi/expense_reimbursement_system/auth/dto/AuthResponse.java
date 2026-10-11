package com.Urvashi.expense_reimbursement_system.auth.dto;

import com.Urvashi.expense_reimbursement_system.user.dto.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
}