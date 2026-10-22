package com.Urvashi.expense_reimbursement_system.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SummaryReport(
        String scope,                      // OWN, TEAM or ALL: whose claims these numbers cover
        LocalDate from,
        LocalDate to,
        String currency,
        long totalClaimsSubmitted,
        long pendingApprovals,             // status SUBMITTED
        long approvedUnpaidCount,          // status MANAGER_APPROVED
        BigDecimal approvedUnpaidAmount,
        long rejectedClaims,
        long reimbursedCount,
        BigDecimal totalReimbursedAmount,
        Long openFlaggedClaims             // null for employees (they can't see flags)
) {
}