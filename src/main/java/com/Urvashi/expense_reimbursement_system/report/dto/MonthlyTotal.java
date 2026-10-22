package com.Urvashi.expense_reimbursement_system.report.dto;

import java.math.BigDecimal;

public record MonthlyTotal(
        String month,          // e.g. "2026-10"
        long claimCount,
        BigDecimal totalAmount
) {
}