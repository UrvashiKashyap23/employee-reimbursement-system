package com.Urvashi.expense_reimbursement_system.audit.dto;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditLog;
import com.Urvashi.expense_reimbursement_system.user.User;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorName,
        AuditAction action,
        String entityType,
        Long entityId,
        String details,
        Instant createdAt
) {

    public static AuditLogResponse from(AuditLog log) {
        User actor = log.getActor();
        return new AuditLogResponse(
                log.getId(),
                actor != null ? actor.getId() : null,
                actor != null ? actor.getFullName() : "System",
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}