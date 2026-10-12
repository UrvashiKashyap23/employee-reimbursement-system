package com.Urvashi.expense_reimbursement_system.policy.dto;

import com.Urvashi.expense_reimbursement_system.policy.ExpensePolicy;

import java.math.BigDecimal;
import java.time.Instant;

public record PolicyResponse(
        Long id,
        Long categoryId,
        BigDecimal maxAmount,
        boolean receiptRequired,
        Integer monthlyClaimLimit,
        boolean active,
        Instant updatedAt,
        String updatedByName
) {

    public static PolicyResponse from(ExpensePolicy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getCategory().getId(),
                policy.getMaxAmount(),
                policy.isReceiptRequired(),
                policy.getMonthlyClaimLimit(),
                policy.isActive(),
                policy.getUpdatedAt(),
                policy.getUpdatedBy() != null ? policy.getUpdatedBy().getFullName() : null
        );
    }
}