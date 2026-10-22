package com.Urvashi.expense_reimbursement_system.reimbursement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ReimbursementRequest(

        @NotBlank(message = "Payment reference is required")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\-_/]{2,99}$",
                message = "Payment reference must be 3 to 100 characters: letters, numbers, - _ /")
        String paymentReference,

        @NotNull(message = "Payment date is required")
        @PastOrPresent(message = "Payment date cannot be in the future")
        LocalDate paymentDate
) {
}