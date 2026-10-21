package com.Urvashi.expense_reimbursement_system.fraud.dto;

import com.Urvashi.expense_reimbursement_system.fraud.ClaimFlag;
import com.Urvashi.expense_reimbursement_system.fraud.FlagStatus;
import com.Urvashi.expense_reimbursement_system.fraud.FlagType;

import java.time.Instant;

public record FlagResponse(
        Long id,
        Long claimId,
        FlagType flagType,
        String reason,
        Long relatedClaimId,
        FlagStatus status,
        String resolvedByName,
        String resolutionNote,
        Instant createdAt,
        Instant resolvedAt
) {

    public static FlagResponse from(ClaimFlag flag) {
        return new FlagResponse(
                flag.getId(),
                flag.getClaim().getId(),
                flag.getFlagType(),
                flag.getReason(),
                flag.getRelatedClaim() != null ? flag.getRelatedClaim().getId() : null,
                flag.getStatus(),
                flag.getResolvedBy() != null ? flag.getResolvedBy().getFullName() : null,
                flag.getResolutionNote(),
                flag.getCreatedAt(),
                flag.getResolvedAt()
        );
    }
}