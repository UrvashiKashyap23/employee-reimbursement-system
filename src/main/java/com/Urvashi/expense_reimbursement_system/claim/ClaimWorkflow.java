package com.Urvashi.expense_reimbursement_system.claim;

import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The single source of truth for which status changes are allowed.
 *
 *  DRAFT ──► SUBMITTED ──► MANAGER_APPROVED ──► REIMBURSED
 *    │          │  │
 *    │          │  └──► REJECTED ──► SUBMITTED (resubmit after editing)
 *    └──────────┴─────────────────► WITHDRAWN   (also from REJECTED)
 *
 * REIMBURSED and WITHDRAWN are final: nothing can change them again.
 */
public final class ClaimWorkflow {

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED = new EnumMap<>(ClaimStatus.class);

    static {
        ALLOWED.put(ClaimStatus.DRAFT,
                EnumSet.of(ClaimStatus.SUBMITTED, ClaimStatus.WITHDRAWN));
        ALLOWED.put(ClaimStatus.SUBMITTED,
                EnumSet.of(ClaimStatus.MANAGER_APPROVED, ClaimStatus.REJECTED, ClaimStatus.WITHDRAWN));
        ALLOWED.put(ClaimStatus.REJECTED,
                EnumSet.of(ClaimStatus.SUBMITTED, ClaimStatus.WITHDRAWN));
        ALLOWED.put(ClaimStatus.MANAGER_APPROVED,
                EnumSet.of(ClaimStatus.REIMBURSED));
        ALLOWED.put(ClaimStatus.REIMBURSED, EnumSet.noneOf(ClaimStatus.class));
        ALLOWED.put(ClaimStatus.WITHDRAWN, EnumSet.noneOf(ClaimStatus.class));
    }

    private ClaimWorkflow() {
        // utility class: nobody should create an object of it
    }

    public static boolean canTransition(ClaimStatus from, ClaimStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    // Throws a 409 Conflict if the change is not allowed
    public static void requireTransition(ClaimStatus from, ClaimStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessRuleException(
                    "A claim cannot move from " + from + " to " + to, HttpStatus.CONFLICT);
        }
    }

    // The employee may edit the details only in these states
    public static boolean isEditable(ClaimStatus status) {
        return status == ClaimStatus.DRAFT || status == ClaimStatus.REJECTED;
    }
}