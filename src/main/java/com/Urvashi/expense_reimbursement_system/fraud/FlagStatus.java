package com.Urvashi.expense_reimbursement_system.fraud;

public enum FlagStatus {
    OPEN,        // waiting for a reviewer
    CLEARED,     // reviewer checked: looks legitimate
    CONFIRMED    // reviewer checked: the problem is real
}