package com.Urvashi.expense_reimbursement_system.claim;

public enum ClaimStatus {
    DRAFT,              // employee is still working on it
    SUBMITTED,          // waiting for manager review
    MANAGER_APPROVED,   // approved, waiting for finance to pay
    REJECTED,           // manager said no (employee may fix and resubmit)
    WITHDRAWN,          // employee cancelled it
    REIMBURSED          // finance has paid it
}