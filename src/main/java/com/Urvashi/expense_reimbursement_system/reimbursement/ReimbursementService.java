package com.Urvashi.expense_reimbursement_system.reimbursement;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditService;
import com.Urvashi.expense_reimbursement_system.claim.ClaimService;
import com.Urvashi.expense_reimbursement_system.claim.ClaimStatus;
import com.Urvashi.expense_reimbursement_system.claim.ClaimStatusHistory;
import com.Urvashi.expense_reimbursement_system.claim.ClaimStatusHistoryRepository;
import com.Urvashi.expense_reimbursement_system.claim.ClaimWorkflow;
import com.Urvashi.expense_reimbursement_system.claim.ExpenseClaim;
import com.Urvashi.expense_reimbursement_system.claim.ExpenseClaimRepository;
import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.common.exception.ResourceNotFoundException;
import com.Urvashi.expense_reimbursement_system.reimbursement.dto.ReimbursementRequest;
import com.Urvashi.expense_reimbursement_system.reimbursement.dto.ReimbursementResponse;
import com.Urvashi.expense_reimbursement_system.user.User;
import com.Urvashi.expense_reimbursement_system.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReimbursementService {

    private static final LocalDate EARLIEST_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate LATEST_DATE = LocalDate.of(2999, 12, 31);

    private final ReimbursementRepository reimbursementRepository;
    private final ExpenseClaimRepository claimRepository;
    private final ClaimStatusHistoryRepository historyRepository;
    private final ClaimService claimService;
    private final UserService userService;
    private final AuditService auditService;

    @Transactional
    public ReimbursementResponse reimburse(Long claimId, ReimbursementRequest request, String financeEmail) {
        User finance = userService.getEntityByEmail(financeEmail);
        ExpenseClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));

        // Separation of duties: you cannot pay yourself
        if (claim.getEmployee().getId().equals(finance.getId())) {
            throw new BusinessRuleException("You cannot reimburse your own claim", HttpStatus.FORBIDDEN);
        }

        // Layer 1: status checks, with clear messages
        if (claim.getStatus() == ClaimStatus.REIMBURSED || reimbursementRepository.existsByClaimId(claimId)) {
            throw new BusinessRuleException("This claim has already been reimbursed", HttpStatus.CONFLICT);
        }
        if (claim.getStatus() != ClaimStatus.MANAGER_APPROVED) {
            throw new BusinessRuleException(
                    "Only manager-approved claims can be reimbursed. This claim is " + claim.getStatus(),
                    HttpStatus.CONFLICT);
        }

        String reference = request.paymentReference().trim().toUpperCase();
        if (reimbursementRepository.existsByPaymentReference(reference)) {
            throw new BusinessRuleException("This payment reference has already been used", HttpStatus.CONFLICT);
        }
        if (request.paymentDate().isBefore(claim.getExpenseDate())) {
            throw new BusinessRuleException("Payment date cannot be before the expense date");
        }

        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setClaim(claim);
        reimbursement.setPaidBy(finance);
        reimbursement.setAmount(claim.getAmount());      // always the claim's amount, never from the request
        reimbursement.setPaymentReference(reference);
        reimbursement.setPaymentDate(request.paymentDate());

        // Layer 2: saveAndFlush runs the INSERT now, so a UNIQUE violation fails immediately
        Reimbursement saved = reimbursementRepository.saveAndFlush(reimbursement);

        // Layer 3: changing the claim bumps its @Version; a concurrent payment fails at commit with 409
        ClaimStatus from = claim.getStatus();
        ClaimWorkflow.requireTransition(from, ClaimStatus.REIMBURSED);
        claim.setStatus(ClaimStatus.REIMBURSED);

        ClaimStatusHistory history = new ClaimStatusHistory();
        history.setClaim(claim);
        history.setFromStatus(from);
        history.setToStatus(ClaimStatus.REIMBURSED);
        history.setActor(finance);
        history.setComment("Paid, reference " + reference);
        historyRepository.save(history);

        auditService.record(finance, AuditAction.REIMBURSEMENT_PROCESSED, "CLAIM", claimId,
                "Paid " + saved.getAmount() + " " + claim.getCurrency() + ", reference " + reference
                        + ", date " + saved.getPaymentDate());
        return ReimbursementResponse.from(saved);
    }

    // Finance history and admin overview, with optional date filters on the payment date
    @Transactional(readOnly = true)
    public PageResponse<ReimbursementResponse> list(LocalDate from, LocalDate to, Pageable pageable) {
        LocalDate start = (from != null) ? from : EARLIEST_DATE;
        LocalDate end = (to != null) ? to : LATEST_DATE;
        if (start.isAfter(end)) {
            throw new BusinessRuleException("'from' date must not be after 'to' date");
        }
        return PageResponse.from(
                reimbursementRepository.findByPaymentDateBetween(start, end, pageable)
                        .map(ReimbursementResponse::from));
    }

    // Payment details of one claim, for anyone who may see that claim (e.g. the employee who was paid)
    @Transactional(readOnly = true)
    public ReimbursementResponse getForClaim(Long claimId, String email) {
        ExpenseClaim claim = claimService.getAccessibleEntity(claimId, email);   // permission check
        Reimbursement reimbursement = reimbursementRepository.findByClaimId(claim.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No reimbursement recorded for claim id: " + claimId));
        return ReimbursementResponse.from(reimbursement);
    }
}