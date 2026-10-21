package com.Urvashi.expense_reimbursement_system.claim;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditService;
import com.Urvashi.expense_reimbursement_system.claim.dto.ClaimRequest;
import com.Urvashi.expense_reimbursement_system.claim.dto.ClaimResponse;
import com.Urvashi.expense_reimbursement_system.claim.dto.StatusHistoryResponse;
import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.common.exception.ResourceNotFoundException;
import com.Urvashi.expense_reimbursement_system.policy.ExpenseCategory;
import com.Urvashi.expense_reimbursement_system.policy.PolicyService;
import com.Urvashi.expense_reimbursement_system.user.Role;
import com.Urvashi.expense_reimbursement_system.user.User;
import com.Urvashi.expense_reimbursement_system.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private static final String DEFAULT_CURRENCY = "INR";

    private final ExpenseClaimRepository claimRepository;
    private final ClaimStatusHistoryRepository historyRepository;
    private final PolicyService policyService;
    private final UserService userService;
    private final AuditService auditService;

    // =====================================================================
    //  Employee actions (on their OWN claims)
    // =====================================================================

    @Transactional
    public ClaimResponse createDraft(ClaimRequest request, String email) {
        User employee = userService.getEntityByEmail(email);

        ExpenseClaim claim = new ExpenseClaim();
        claim.setEmployee(employee);
        applyRequest(claim, request);
        claim.setStatus(ClaimStatus.DRAFT);
        ExpenseClaim saved = claimRepository.save(claim);

        recordHistory(saved, null, ClaimStatus.DRAFT, employee, "Draft created");
        auditService.record(employee, AuditAction.CLAIM_CREATED, "CLAIM", saved.getId(),
                "Draft created: " + saved.getAmount() + " " + saved.getCurrency()
                        + " at " + saved.getMerchant());
        return ClaimResponse.from(saved);
    }

    @Transactional
    public ClaimResponse updateClaim(Long id, ClaimRequest request, String email) {
        User user = userService.getEntityByEmail(email);
        ExpenseClaim claim = findClaim(id);
        requireOwner(claim, user);

        if (!ClaimWorkflow.isEditable(claim.getStatus())) {
            throw new BusinessRuleException(
                    "Only DRAFT or REJECTED claims can be edited. This claim is " + claim.getStatus(),
                    HttpStatus.CONFLICT);
        }

        applyRequest(claim, request);   // Hibernate's dirty checking runs the UPDATE at commit
        auditService.record(user, AuditAction.CLAIM_EDITED, "CLAIM", claim.getId(),
                "Claim edited while " + claim.getStatus());
        return ClaimResponse.from(claim);
    }

    // Used for the first submission (DRAFT) and for resubmitting after a rejection (REJECTED)
    @Transactional
    public ClaimResponse submitClaim(Long id, String email) {
        User user = userService.getEntityByEmail(email);
        ExpenseClaim claim = findClaim(id);
        requireOwner(claim, user);

        ClaimStatus from = claim.getStatus();
        ClaimWorkflow.requireTransition(from, ClaimStatus.SUBMITTED);

        User manager = user.getManager();
        if (manager == null) {
            throw new BusinessRuleException("You have no manager assigned. Ask an admin to assign one.");
        }
        if (!manager.isActive() || manager.getRole() != Role.MANAGER) {
            throw new BusinessRuleException("Your assigned manager is not available. Ask an admin to update it.");
        }

        boolean resubmission = (from == ClaimStatus.REJECTED);
        claim.setAssignedManager(manager);
        claim.setSubmittedAt(Instant.now());
        changeStatus(claim, ClaimStatus.SUBMITTED, user,
                resubmission ? "Resubmitted after changes" : "Submitted for review");

        auditService.record(user, resubmission ? AuditAction.CLAIM_RESUBMITTED : AuditAction.CLAIM_SUBMITTED,
                "CLAIM", claim.getId(), "Assigned to manager " + manager.getFullName());
        return ClaimResponse.from(claim);
    }

    @Transactional
    public ClaimResponse withdrawClaim(Long id, String email) {
        User user = userService.getEntityByEmail(email);
        ExpenseClaim claim = findClaim(id);
        requireOwner(claim, user);

        changeStatus(claim, ClaimStatus.WITHDRAWN, user, "Withdrawn by employee");
        auditService.record(user, AuditAction.CLAIM_WITHDRAWN, "CLAIM", claim.getId(), "Claim withdrawn");
        return ClaimResponse.from(claim);
    }

    // =====================================================================
    //  Manager actions
    // =====================================================================

    @Transactional
    public ClaimResponse approveClaim(Long id, String comment, String reviewerEmail) {
        User reviewer = userService.getEntityByEmail(reviewerEmail);
        ExpenseClaim claim = findClaim(id);
        requireAssignedReviewer(claim, reviewer);

        changeStatus(claim, ClaimStatus.MANAGER_APPROVED, reviewer, blankToNull(comment));
        auditService.record(reviewer, AuditAction.CLAIM_APPROVED, "CLAIM", claim.getId(),
                "Approved by " + reviewer.getFullName() + commentSuffix(comment));
        return ClaimResponse.from(claim);
    }

    @Transactional
    public ClaimResponse rejectClaim(Long id, String comment, String reviewerEmail) {
        User reviewer = userService.getEntityByEmail(reviewerEmail);
        ExpenseClaim claim = findClaim(id);
        requireAssignedReviewer(claim, reviewer);

        if (blankToNull(comment) == null) {
            throw new BusinessRuleException("A reason is required when rejecting a claim");
        }

        changeStatus(claim, ClaimStatus.REJECTED, reviewer, comment.trim());
        auditService.record(reviewer, AuditAction.CLAIM_REJECTED, "CLAIM", claim.getId(),
                "Rejected by " + reviewer.getFullName() + commentSuffix(comment));
        return ClaimResponse.from(claim);
    }

    // =====================================================================
    //  Reading
    // =====================================================================

    @Transactional(readOnly = true)
    public ClaimResponse getClaim(Long id, String email) {
        return ClaimResponse.from(getAccessibleEntity(id, email));
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getHistory(Long id, String email) {
        ExpenseClaim claim = getAccessibleEntity(id, email);
        return historyRepository.findByClaimIdOrderByCreatedAtAsc(claim.getId()).stream()
                .map(StatusHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listMyClaims(String email, ClaimStatus status, Pageable pageable) {
        User user = userService.getEntityByEmail(email);
        Page<ExpenseClaim> page = (status == null)
                ? claimRepository.findByEmployeeId(user.getId(), pageable)
                : claimRepository.findByEmployeeIdAndStatus(user.getId(), status, pageable);
        return PageResponse.from(page.map(ClaimResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listManagerClaims(String email, ClaimStatus status, Pageable pageable) {
        User manager = userService.getEntityByEmail(email);
        Page<ExpenseClaim> page = (status == null)
                ? claimRepository.findByAssignedManagerId(manager.getId(), pageable)
                : claimRepository.findByAssignedManagerIdAndStatus(manager.getId(), status, pageable);
        return PageResponse.from(page.map(ClaimResponse::from));
    }

    // Finance only sees claims that have passed manager approval (or are already paid)
    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listFinanceClaims(ClaimStatus status, Pageable pageable) {
        ClaimStatus filter = (status == null) ? ClaimStatus.MANAGER_APPROVED : status;
        if (filter != ClaimStatus.MANAGER_APPROVED && filter != ClaimStatus.REIMBURSED) {
            throw new BusinessRuleException("Finance can only list MANAGER_APPROVED or REIMBURSED claims");
        }
        return PageResponse.from(claimRepository.findByStatus(filter, pageable).map(ClaimResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listAllClaims(ClaimStatus status, Pageable pageable) {
        Page<ExpenseClaim> page = (status == null)
                ? claimRepository.findAll(pageable)
                : claimRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(ClaimResponse::from));
    }

    // =====================================================================
    //  Helpers that later modules (receipt, fraud, reimbursement) will reuse
    // =====================================================================

    // Returns the claim only if this user is allowed to see it, otherwise 403
    @Transactional(readOnly = true)
    public ExpenseClaim getAccessibleEntity(Long claimId, String email) {
        User user = userService.getEntityByEmail(email);
        ExpenseClaim claim = findClaim(claimId);
        if (!canView(claim, user)) {
            throw new AccessDeniedException("You do not have access to this claim");
        }
        return claim;
    }

    // =====================================================================
    //  Private helpers
    // =====================================================================

    private ExpenseClaim findClaim(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", id));
    }

    private void applyRequest(ExpenseClaim claim, ClaimRequest request) {
        ExpenseCategory category = policyService.getCategoryEntity(request.categoryId());
        if (!category.isActive()) {
            throw new BusinessRuleException("This expense category is not active");
        }

        String currency = (request.currency() == null) ? DEFAULT_CURRENCY : request.currency();
        if (!DEFAULT_CURRENCY.equals(currency)) {
            throw new BusinessRuleException("Only INR claims are supported for now");
        }

        claim.setCategory(category);
        claim.setAmount(request.amount());
        claim.setCurrency(currency);
        claim.setExpenseDate(request.expenseDate());
        claim.setMerchant(request.merchant().trim());
        claim.setReceiptReference(blankToNull(request.receiptReference()));
        claim.setBusinessPurpose(request.businessPurpose().trim());
    }

    // The ONLY place where a claim's status is changed: checks the state machine, then writes history
    private void changeStatus(ExpenseClaim claim, ClaimStatus to, User actor, String comment) {
        ClaimStatus from = claim.getStatus();
        ClaimWorkflow.requireTransition(from, to);
        claim.setStatus(to);
        recordHistory(claim, from, to, actor, comment);
    }

    private void recordHistory(ExpenseClaim claim, ClaimStatus from, ClaimStatus to, User actor, String comment) {
        ClaimStatusHistory history = new ClaimStatusHistory();
        history.setClaim(claim);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setActor(actor);
        history.setComment(comment);
        historyRepository.save(history);
    }

    private void requireOwner(ExpenseClaim claim, User user) {
        if (!claim.getEmployee().getId().equals(user.getId())) {
            throw new AccessDeniedException("This is not your claim");
        }
    }

    private void requireAssignedReviewer(ExpenseClaim claim, User reviewer) {
        if (claim.getEmployee().getId().equals(reviewer.getId())) {
            throw new BusinessRuleException("You cannot review your own claim", HttpStatus.FORBIDDEN);
        }
        User assigned = claim.getAssignedManager();
        if (assigned == null || !assigned.getId().equals(reviewer.getId())) {
            throw new AccessDeniedException("This claim is not assigned to you");
        }
    }

    private boolean canView(ExpenseClaim claim, User user) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (claim.getEmployee().getId().equals(user.getId())) {
            return true;
        }
        if (user.getRole() == Role.MANAGER && claim.getAssignedManager() != null
                && claim.getAssignedManager().getId().equals(user.getId())) {
            return true;
        }
        return user.getRole() == Role.FINANCE
                && (claim.getStatus() == ClaimStatus.MANAGER_APPROVED
                || claim.getStatus() == ClaimStatus.REIMBURSED);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String commentSuffix(String comment) {
        String cleaned = blankToNull(comment);
        return cleaned == null ? "" : ": " + cleaned;
    }
}