package com.Urvashi.expense_reimbursement_system.claim.dto;

import com.Urvashi.expense_reimbursement_system.claim.ClaimStatus;
import com.Urvashi.expense_reimbursement_system.claim.ClaimWorkflow;
import com.Urvashi.expense_reimbursement_system.claim.ExpenseClaim;
import com.Urvashi.expense_reimbursement_system.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ClaimResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        String merchant,
        String receiptReference,
        String businessPurpose,
        ClaimStatus status,
        boolean editable,
        Long assignedManagerId,
        String assignedManagerName,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static ClaimResponse from(ExpenseClaim claim) {
        User manager = claim.getAssignedManager();
        return new ClaimResponse(
                claim.getId(),
                claim.getEmployee().getId(),
                claim.getEmployee().getFullName(),
                claim.getCategory().getId(),
                claim.getCategory().getName(),
                claim.getAmount(),
                claim.getCurrency(),
                claim.getExpenseDate(),
                claim.getMerchant(),
                claim.getReceiptReference(),
                claim.getBusinessPurpose(),
                claim.getStatus(),
                ClaimWorkflow.isEditable(claim.getStatus()),
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFullName() : null,
                claim.getSubmittedAt(),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }
}