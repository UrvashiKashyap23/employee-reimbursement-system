package com.Urvashi.expense_reimbursement_system.fraud;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditService;
import com.Urvashi.expense_reimbursement_system.claim.ClaimService;
import com.Urvashi.expense_reimbursement_system.claim.ExpenseClaim;
import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.common.exception.ResourceNotFoundException;
import com.Urvashi.expense_reimbursement_system.fraud.dto.FlagResponse;
import com.Urvashi.expense_reimbursement_system.fraud.dto.ResolveFlagRequest;
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
public class FlagService {

    private final ClaimFlagRepository flagRepository;
    private final ClaimService claimService;
    private final UserService userService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<FlagResponse> listForClaim(Long claimId, String email) {
        User user = userService.getEntityByEmail(email);
        ExpenseClaim claim = claimService.getAccessibleEntity(claimId, email);   // may this user see the claim?
        if (claim.getEmployee().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot view review flags on your own claim");
        }
        return flagRepository.findByClaimIdOrderByCreatedAtAsc(claim.getId()).stream()
                .map(FlagResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<FlagResponse> listManagerFlags(String email, FlagStatus status, Pageable pageable) {
        User manager = userService.getEntityByEmail(email);
        Page<ClaimFlag> page = (status == null)
                ? flagRepository.findByClaim_AssignedManager_Id(manager.getId(), pageable)
                : flagRepository.findByClaim_AssignedManager_IdAndStatus(manager.getId(), status, pageable);
        return PageResponse.from(page.map(FlagResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<FlagResponse> listAllFlags(FlagStatus status, Pageable pageable) {
        Page<ClaimFlag> page = (status == null)
                ? flagRepository.findAll(pageable)
                : flagRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(FlagResponse::from));
    }

    @Transactional
    public FlagResponse resolve(Long flagId, ResolveFlagRequest request, String email) {
        User reviewer = userService.getEntityByEmail(email);
        ClaimFlag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", flagId));
        ExpenseClaim claim = flag.getClaim();

        if (claim.getEmployee().getId().equals(reviewer.getId())) {
            throw new BusinessRuleException("You cannot resolve flags on your own claim", HttpStatus.FORBIDDEN);
        }
        User assigned = claim.getAssignedManager();
        if (assigned == null || !assigned.getId().equals(reviewer.getId())) {
            throw new AccessDeniedException("This flag is not on a claim assigned to you");
        }
        if (request.status() == FlagStatus.OPEN) {
            throw new BusinessRuleException("Resolution must be CLEARED or CONFIRMED");
        }
        if (flag.getStatus() != FlagStatus.OPEN) {
            throw new BusinessRuleException("This flag has already been resolved", HttpStatus.CONFLICT);
        }

        flag.setStatus(request.status());
        flag.setResolvedBy(reviewer);
        flag.setResolutionNote(request.note().trim());
        flag.setResolvedAt(Instant.now());

        auditService.record(reviewer, AuditAction.FLAG_RESOLVED, "CLAIM", claim.getId(),
                flag.getFlagType() + " marked " + request.status() + ": " + flag.getResolutionNote());
        return FlagResponse.from(flag);
    }
}