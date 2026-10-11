package com.Urvashi.expense_reimbursement_system.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Override
    @EntityGraph(attributePaths = "actor")
    Page<AuditLog> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "actor")
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    @EntityGraph(attributePaths = "actor")
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);
}