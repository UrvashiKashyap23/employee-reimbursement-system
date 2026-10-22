package com.Urvashi.expense_reimbursement_system.reimbursement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, Long> {

    boolean existsByClaimId(Long claimId);

    boolean existsByPaymentReference(String paymentReference);

    @EntityGraph(attributePaths = {"claim", "claim.employee", "claim.category", "paidBy"})
    Optional<Reimbursement> findByClaimId(Long claimId);

    @EntityGraph(attributePaths = {"claim", "claim.employee", "claim.category", "paidBy"})
    Page<Reimbursement> findByPaymentDateBetween(LocalDate from, LocalDate to, Pageable pageable);
}