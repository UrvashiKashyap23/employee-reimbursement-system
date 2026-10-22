package com.Urvashi.expense_reimbursement_system.reimbursement.dto;

import com.Urvashi.expense_reimbursement_system.reimbursement.Reimbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReimbursementResponse(
        Long id,
        Long claimId,
        String employeeName,
        String categoryName,
        BigDecimal amount,
        String currency,
        String paymentReference,
        LocalDate paymentDate,
        Long paidById,
        String paidByName,
        Instant createdAt
) {

    public static ReimbursementResponse from(Reimbursement reimbursement) {
        return new ReimbursementResponse(
                reimbursement.getId(),
                reimbursement.getClaim().getId(),
                reimbursement.getClaim().getEmployee().getFullName(),
                reimbursement.getClaim().getCategory().getName(),
                reimbursement.getAmount(),
                reimbursement.getClaim().getCurrency(),
                reimbursement.getPaymentReference(),
                reimbursement.getPaymentDate(),
                reimbursement.getPaidBy().getId(),
                reimbursement.getPaidBy().getFullName(),
                reimbursement.getCreatedAt()
        );
    }
}