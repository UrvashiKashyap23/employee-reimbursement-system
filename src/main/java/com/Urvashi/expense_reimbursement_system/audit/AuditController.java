package com.Urvashi.expense_reimbursement_system.audit;

import com.Urvashi.expense_reimbursement_system.audit.dto.AuditLogResponse;
import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    // GET /api/admin/audit-logs?action=CLAIM_APPROVED&page=0&size=20
    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) AuditAction action,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditService.list(action, pageable);
    }

    // GET /api/admin/audit-logs/CLAIM/12  -> full history of claim 12
    @GetMapping("/{entityType}/{entityId}")
    public List<AuditLogResponse> listForEntity(@PathVariable String entityType,
                                                @PathVariable Long entityId) {
        return auditService.listForEntity(entityType, entityId);
    }
}