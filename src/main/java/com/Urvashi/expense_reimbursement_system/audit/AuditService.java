package com.Urvashi.expense_reimbursement_system.audit;

import com.Urvashi.expense_reimbursement_system.audit.dto.AuditLogResponse;
import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import com.Urvashi.expense_reimbursement_system.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_DETAILS_LENGTH = 500;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(User actor, AuditAction action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(truncate(details));
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(AuditAction action, Pageable pageable) {
        Page<AuditLog> page = (action == null)
                ? auditLogRepository.findAll(pageable)
                : auditLogRepository.findByAction(action, pageable);
        return PageResponse.from(page.map(AuditLogResponse::from));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listForEntity(String entityType, Long entityId) {
        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType.toUpperCase(), entityId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_DETAILS_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_DETAILS_LENGTH - 3) + "...";
    }
}