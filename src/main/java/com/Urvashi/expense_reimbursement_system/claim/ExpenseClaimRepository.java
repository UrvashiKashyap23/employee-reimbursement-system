package com.Urvashi.expense_reimbursement_system.claim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {

    // @EntityGraph loads the employee, category and manager in the SAME query as the claim,
    // so listing 20 claims does not trigger 60 extra queries (the "N+1 problem").

    @Override
    @EntityGraph(attributePaths = {"employee", "category", "assignedManager"})
    Optional<ExpenseClaim> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"employee", "category", "assignedManager"})
    Page<ExpenseClaim> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "category", "assignedManager"})
    Page<ExpenseClaim> findByEmployeeId(Long employeeId, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "category", "assignedManager"})
    Page<ExpenseClaim> findByEmployeeIdAndStatus(Long employeeId, ClaimStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "category", "assignedManager"})
    Page<ExpenseClaim> findByAssignedManagerId(Long managerId, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "category", "assignedManager"})
    Page<ExpenseClaim> findByAssignedManagerIdAndStatus(Long managerId, ClaimStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "category", "assignedManager"})
    Page<ExpenseClaim> findByStatus(ClaimStatus status, Pageable pageable);

    // ---------- NEW: used by duplicate detection and policy checks ----------

    // Same employee + merchant + date + amount on a different, "live" claim
    @Query("""
            select c from ExpenseClaim c
            where c.employee.id = :employeeId
              and c.id <> :claimId
              and c.expenseDate = :expenseDate
              and c.amount = :amount
              and lower(c.merchant) = lower(:merchant)
              and c.status in :statuses
            """)
    List<ExpenseClaim> findPossibleDuplicates(@Param("employeeId") Long employeeId,
                                              @Param("claimId") Long claimId,
                                              @Param("expenseDate") LocalDate expenseDate,
                                              @Param("amount") BigDecimal amount,
                                              @Param("merchant") String merchant,
                                              @Param("statuses") Collection<ClaimStatus> statuses);

    // Same merchant + same receipt reference on a different claim (any employee)
    @Query("""
            select c from ExpenseClaim c
            where c.id <> :claimId
              and lower(c.merchant) = lower(:merchant)
              and lower(c.receiptReference) = lower(:reference)
              and c.status in :statuses
            """)
    List<ExpenseClaim> findSameReceiptReference(@Param("claimId") Long claimId,
                                                @Param("merchant") String merchant,
                                                @Param("reference") String reference,
                                                @Param("statuses") Collection<ClaimStatus> statuses);

    // Monthly claim limit (per category)
    long countByEmployeeIdAndCategoryIdAndSubmittedAtBetweenAndIdNot(Long employeeId, Long categoryId,
                                                                     Instant from, Instant to, Long claimId);
}