package com.Urvashi.expense_reimbursement_system.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findAllByOrderByNameAsc();

    List<ExpenseCategory> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<ExpenseCategory> findByNameIgnoreCase(String name);
}