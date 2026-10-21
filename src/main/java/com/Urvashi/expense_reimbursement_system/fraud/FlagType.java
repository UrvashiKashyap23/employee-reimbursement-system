package com.Urvashi.expense_reimbursement_system.fraud;

public enum FlagType {
    DUPLICATE_RECEIPT_HASH,   // identical receipt file already used on another claim
    DUPLICATE_REFERENCE,      // same merchant + receipt/transaction reference on another claim
    DUPLICATE_DETAILS,        // same employee + merchant + date + amount on another claim
    POLICY_VIOLATION          // breaks an admin-defined policy rule
}