package com.Urvashi.expense_reimbursement_system.policy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PolicyRequest(

        @DecimalMin(value = "0.01", message = "Maximum amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Maximum amount can have at most 10 digits and 2 decimals")
        BigDecimal maxAmount,            // null = no maximum

        @NotNull(message = "receiptRequired must be true or false")
        Boolean receiptRequired,

        @Min(value = 1, message = "Monthly claim limit must be at least 1")
        Integer monthlyClaimLimit,       // null = no limit

        Boolean active                   // null = unchanged (new policies start active)
) {
}