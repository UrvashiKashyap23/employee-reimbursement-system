package com.Urvashi.expense_reimbursement_system.claim.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(

        @NotNull(message = "Category is required")
        Long categoryId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Amount can have at most 10 digits and 2 decimals")
        BigDecimal amount,

        // Optional: defaults to INR. Only INR is supported for now.
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code such as INR")
        String currency,

        @NotNull(message = "Expense date is required")
        @PastOrPresent(message = "Expense date cannot be in the future")
        LocalDate expenseDate,

        @NotBlank(message = "Merchant is required")
        @Size(max = 150, message = "Merchant must be at most 150 characters")
        String merchant,

        @Size(max = 100, message = "Receipt reference must be at most 100 characters")
        String receiptReference,

        @NotBlank(message = "Business purpose is required")
        @Size(max = 500, message = "Business purpose must be at most 500 characters")
        String businessPurpose
) {
}