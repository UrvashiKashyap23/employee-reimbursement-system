package com.Urvashi.expense_reimbursement_system.policy;

import com.Urvashi.expense_reimbursement_system.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "expense_policies")
@Getter
@Setter
@NoArgsConstructor
public class ExpensePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One policy per category: unique = true is enforced by PostgreSQL
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, unique = true)
    private ExpenseCategory category;

    // Money: BigDecimal in Java, NUMERIC(12,2) in PostgreSQL. null = no maximum
    @Column(name = "max_amount", precision = 12, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "receipt_required", nullable = false)
    private boolean receiptRequired;

    // Max number of claims one employee may submit per calendar month. null = no limit
    @Column(name = "monthly_claim_limit")
    private Integer monthlyClaimLimit;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}