package com.Urvashi.expense_reimbursement_system.fraud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimFlagRepository extends JpaRepository<ClaimFlag, Long> {

    @Override
    @EntityGraph(attributePaths = {"claim", "relatedClaim", "resolvedBy"})
    Optional<ClaimFlag> findById(Long id);

    @EntityGraph(attributePaths = {"claim", "relatedClaim", "resolvedBy"})
    List<ClaimFlag> findByClaimIdOrderByCreatedAtAsc(Long claimId);

    boolean existsByClaimIdAndStatus(Long claimId, FlagStatus status);

    // Flags on claims assigned to one manager (the manager's review queue)
    @EntityGraph(attributePaths = {"claim", "relatedClaim", "resolvedBy"})
    Page<ClaimFlag> findByClaim_AssignedManager_Id(Long managerId, Pageable pageable);

    @EntityGraph(attributePaths = {"claim", "relatedClaim", "resolvedBy"})
    Page<ClaimFlag> findByClaim_AssignedManager_IdAndStatus(Long managerId, FlagStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"claim", "relatedClaim", "resolvedBy"})
    Page<ClaimFlag> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"claim", "relatedClaim", "resolvedBy"})
    Page<ClaimFlag> findByStatus(FlagStatus status, Pageable pageable);
}