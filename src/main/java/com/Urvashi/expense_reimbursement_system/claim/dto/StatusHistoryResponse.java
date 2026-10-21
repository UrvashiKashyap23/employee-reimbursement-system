package com.Urvashi.expense_reimbursement_system.claim.dto;

import com.Urvashi.expense_reimbursement_system.claim.ClaimStatus;
import com.Urvashi.expense_reimbursement_system.claim.ClaimStatusHistory;

import java.time.Instant;

public record StatusHistoryResponse(
        Long id,
        ClaimStatus fromStatus,
        ClaimStatus toStatus,
        Long actorId,
        String actorName,
        String comment,
        Instant createdAt
) {

    public static StatusHistoryResponse from(ClaimStatusHistory history) {
        return new StatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getActor().getId(),
                history.getActor().getFullName(),
                history.getComment(),
                history.getCreatedAt()
        );
    }
}