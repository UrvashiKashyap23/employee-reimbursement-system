package com.Urvashi.expense_reimbursement_system.user.dto;

import com.Urvashi.expense_reimbursement_system.user.Role;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserRequest(
        @NotNull(message = "Role is required")
        Role role,

        Long managerId,   // optional: null means "no manager"

        @NotNull(message = "Active flag is required")
        Boolean active
) {
}