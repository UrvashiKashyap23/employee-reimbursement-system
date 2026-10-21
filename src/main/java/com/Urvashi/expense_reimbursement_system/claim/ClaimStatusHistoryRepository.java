package com.Urvashi.expense_reimbursement_system.claim;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Long> {

    @EntityGraph(attributePaths = "actor")
    List<ClaimStatusHistory> findByClaimIdOrderByCreatedAtAsc(Long claimId);
}