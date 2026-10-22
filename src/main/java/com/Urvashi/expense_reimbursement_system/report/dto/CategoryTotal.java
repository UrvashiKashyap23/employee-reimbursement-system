package com.Urvashi.expense_reimbursement_system.report.dto;

import java.math.BigDecimal;

public record CategoryTotal(
        Long categoryId,
        String categoryName,
        long claimCount,
        BigDecimal totalAmount
) {
}