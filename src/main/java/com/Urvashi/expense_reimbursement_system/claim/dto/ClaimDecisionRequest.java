package com.Urvashi.expense_reimbursement_system.claim.dto;

import jakarta.validation.constraints.Size;

public record ClaimDecisionRequest(

        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}