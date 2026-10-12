package com.Urvashi.expense_reimbursement_system.policy.dto;

public record CategoryResponse(
        Long id,
        String name,
        boolean active,
        PolicyResponse policy
) {
}