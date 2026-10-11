package com.Urvashi.expense_reimbursement_system.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring builds the SQL from the method name: SELECT ... WHERE email = ?
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Load each user's manager in the same query (avoids the "N+1 queries" problem)
    @Override
    @EntityGraph(attributePaths = "manager")
    Page<User> findAll(Pageable pageable);
}