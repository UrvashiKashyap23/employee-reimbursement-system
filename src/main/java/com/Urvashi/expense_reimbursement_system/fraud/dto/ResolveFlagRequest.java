package com.Urvashi.expense_reimbursement_system.fraud.dto;

import com.Urvashi.expense_reimbursement_system.fraud.FlagStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveFlagRequest(

        @NotNull(message = "Status is required (CLEARED or CONFIRMED)")
        FlagStatus status,

        @NotBlank(message = "A note explaining your decision is required")
        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}