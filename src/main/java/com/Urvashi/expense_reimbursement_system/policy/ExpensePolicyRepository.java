package com.Urvashi.expense_reimbursement_system.policy;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpensePolicyRepository extends JpaRepository<ExpensePolicy, Long> {

    @EntityGraph(attributePaths = "updatedBy")
    Optional<ExpensePolicy> findByCategoryId(Long categoryId);

    @Override
    @EntityGraph(attributePaths = {"category", "updatedBy"})
    List<ExpensePolicy> findAll();
}