package com.Urvashi.expense_reimbursement_system.claim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
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

    // Used for the "monthly claim limit" policy rule
    long countByEmployeeIdAndSubmittedAtBetweenAndIdNot(Long employeeId, Instant from, Instant to, Long claimId);
}