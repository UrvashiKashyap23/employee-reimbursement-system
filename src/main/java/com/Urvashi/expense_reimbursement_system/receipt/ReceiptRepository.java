package com.Urvashi.expense_reimbursement_system.receipt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findByClaimIdOrderByUploadedAtAsc(Long claimId);

    long countByClaimId(Long claimId);

    boolean existsByClaimId(Long claimId);

    // Used by duplicate detection (fraud step): same file content attached to a DIFFERENT claim
    List<Receipt> findBySha256HashAndClaimIdNot(String sha256Hash, Long claimId);
}