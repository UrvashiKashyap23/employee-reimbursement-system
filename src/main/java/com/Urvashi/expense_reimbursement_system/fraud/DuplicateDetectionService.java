package com.Urvashi.expense_reimbursement_system.fraud;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditService;
import com.Urvashi.expense_reimbursement_system.claim.ClaimStatus;
import com.Urvashi.expense_reimbursement_system.claim.ExpenseClaim;
import com.Urvashi.expense_reimbursement_system.claim.ExpenseClaimRepository;
import com.Urvashi.expense_reimbursement_system.policy.ExpensePolicy;
import com.Urvashi.expense_reimbursement_system.policy.PolicyService;
import com.Urvashi.expense_reimbursement_system.receipt.Receipt;
import com.Urvashi.expense_reimbursement_system.receipt.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    // We only compare against claims that are "live": under review, approved or already paid
    private static final List<ClaimStatus> COMPARABLE_STATUSES =
            List.of(ClaimStatus.SUBMITTED, ClaimStatus.MANAGER_APPROVED, ClaimStatus.REIMBURSED);

    private final ExpenseClaimRepository claimRepository;
    private final ReceiptRepository receiptRepository;
    private final ClaimFlagRepository flagRepository;
    private final PolicyService policyService;
    private final AuditService auditService;

    // One problem found by a rule (not saved yet)
    private record Finding(FlagType type, String reason, ExpenseClaim related) {
    }

    /**
     * Runs all rules for a claim that was just submitted and saves a flag for each new problem.
     * Returns how many NEW flags were created. A flag never rejects the claim.
     */
    @Transactional
    public int checkAndFlag(ExpenseClaim claim) {
        List<Finding> findings = new ArrayList<>();
        findings.addAll(checkReceiptHashes(claim));
        findings.addAll(checkReceiptReference(claim));
        findings.addAll(checkSameDetails(claim));
        findings.addAll(checkPolicy(claim));
        return saveNewFlags(claim, findings);
    }

    // ---------- rule 1: same receipt file ----------

    private List<Finding> checkReceiptHashes(ExpenseClaim claim) {
        List<Finding> findings = new ArrayList<>();
        for (Receipt mine : receiptRepository.findByClaimIdOrderByUploadedAtAsc(claim.getId())) {
            for (Receipt other : receiptRepository.findBySha256HashAndClaimIdNot(mine.getSha256Hash(), claim.getId())) {
                ExpenseClaim related = other.getClaim();
                if (COMPARABLE_STATUSES.contains(related.getStatus())) {
                    findings.add(new Finding(FlagType.DUPLICATE_RECEIPT_HASH,
                            "Receipt file '" + mine.getOriginalFilename()
                                    + "' is identical to a receipt on claim #" + related.getId(),
                            related));
                }
            }
        }
        return findings;
    }

    // ---------- rule 2: same merchant + same receipt reference ----------

    private List<Finding> checkReceiptReference(ExpenseClaim claim) {
        List<Finding> findings = new ArrayList<>();
        String reference = claim.getReceiptReference();
        if (reference == null || reference.isBlank()) {
            return findings;   // nothing to compare
        }
        for (ExpenseClaim related : claimRepository.findSameReceiptReference(
                claim.getId(), claim.getMerchant(), reference, COMPARABLE_STATUSES)) {
            findings.add(new Finding(FlagType.DUPLICATE_REFERENCE,
                    "Receipt reference '" + reference + "' at " + claim.getMerchant()
                            + " was already used on claim #" + related.getId(),
                    related));
        }
        return findings;
    }

    // ---------- rule 3: same employee + merchant + date + amount ----------

    private List<Finding> checkSameDetails(ExpenseClaim claim) {
        List<Finding> findings = new ArrayList<>();
        for (ExpenseClaim related : claimRepository.findPossibleDuplicates(
                claim.getEmployee().getId(), claim.getId(), claim.getExpenseDate(),
                claim.getAmount(), claim.getMerchant(), COMPARABLE_STATUSES)) {
            findings.add(new Finding(FlagType.DUPLICATE_DETAILS,
                    "Same employee, merchant, date and amount as claim #" + related.getId(),
                    related));
        }
        return findings;
    }

    // ---------- rule 4: admin-defined policy ----------

    private List<Finding> checkPolicy(ExpenseClaim claim) {
        List<Finding> findings = new ArrayList<>();
        Optional<ExpensePolicy> found = policyService.findActivePolicy(claim.getCategory().getId());
        if (found.isEmpty()) {
            return findings;   // no active policy for this category: nothing to enforce
        }
        ExpensePolicy policy = found.get();
        String category = claim.getCategory().getName();

        if (policy.getMaxAmount() != null && claim.getAmount().compareTo(policy.getMaxAmount()) > 0) {
            findings.add(new Finding(FlagType.POLICY_VIOLATION,
                    "Amount " + claim.getAmount() + " exceeds the " + category
                            + " limit of " + policy.getMaxAmount(), null));
        }

        if (policy.isReceiptRequired() && !receiptRepository.existsByClaimId(claim.getId())) {
            findings.add(new Finding(FlagType.POLICY_VIOLATION,
                    "A receipt is required for " + category + " claims but none is attached", null));
        }

        if (policy.getMonthlyClaimLimit() != null) {
            ZoneId zone = ZoneId.systemDefault();
            YearMonth month = YearMonth.now(zone);
            Instant from = month.atDay(1).atStartOfDay(zone).toInstant();
            Instant to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
            long alreadySubmitted = claimRepository.countByEmployeeIdAndCategoryIdAndSubmittedAtBetweenAndIdNot(
                    claim.getEmployee().getId(), claim.getCategory().getId(), from, to, claim.getId());
            if (alreadySubmitted >= policy.getMonthlyClaimLimit()) {
                findings.add(new Finding(FlagType.POLICY_VIOLATION,
                        "Monthly limit of " + policy.getMonthlyClaimLimit() + " " + category
                                + " claims reached (" + alreadySubmitted + " already submitted this month)", null));
            }
        }
        return findings;
    }

    // ---------- saving ----------

    private int saveNewFlags(ExpenseClaim claim, List<Finding> findings) {
        List<ClaimFlag> existing = flagRepository.findByClaimIdOrderByCreatedAtAsc(claim.getId());
        int created = 0;

        for (Finding finding : findings) {
            Long relatedId = finding.related() != null ? finding.related().getId() : null;

            // Skip if this exact problem is already recorded (open OR already reviewed by a human),
            // so resubmitting does not re-raise something a reviewer already decided on
            boolean alreadyRecorded = existing.stream().anyMatch(e ->
                    e.getFlagType() == finding.type()
                            && Objects.equals(e.getRelatedClaim() != null ? e.getRelatedClaim().getId() : null, relatedId)
                            && e.getReason().equals(finding.reason()));
            if (alreadyRecorded) {
                continue;
            }

            ClaimFlag flag = new ClaimFlag();
            flag.setClaim(claim);
            flag.setFlagType(finding.type());
            flag.setReason(finding.reason());
            flag.setRelatedClaim(finding.related());
            flag.setStatus(FlagStatus.OPEN);
            flagRepository.save(flag);

            // Actor is null: the SYSTEM raised this flag, not a person
            auditService.record(null, AuditAction.CLAIM_FLAGGED, "CLAIM", claim.getId(),
                    finding.type() + ": " + finding.reason());
            created++;
        }
        return created;
    }
}