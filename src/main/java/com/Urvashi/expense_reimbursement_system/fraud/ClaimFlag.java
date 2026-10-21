package com.Urvashi.expense_reimbursement_system.fraud;

import com.Urvashi.expense_reimbursement_system.claim.ExpenseClaim;
import com.Urvashi.expense_reimbursement_system.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "claim_flags", indexes = {
        @Index(name = "idx_flag_claim", columnList = "claim_id"),
        @Index(name = "idx_flag_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class ClaimFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The claim that was flagged
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private ExpenseClaim claim;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 30)
    private FlagType flagType;

    // Plain-English explanation shown to the reviewer
    @Column(nullable = false, length = 500)
    private String reason;

    // For duplicate flags: the other claim it matched (null for policy flags)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_claim_id")
    private ExpenseClaim relatedClaim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlagStatus status = FlagStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}