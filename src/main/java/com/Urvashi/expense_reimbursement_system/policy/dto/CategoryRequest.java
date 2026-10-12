package com.Urvashi.expense_reimbursement_system.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 60, message = "Category name must be at most 60 characters")
        String name,

        Boolean active   // optional: on create defaults to true; on update null means "unchanged"
) {
}