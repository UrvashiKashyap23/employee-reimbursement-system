package com.Urvashi.expense_reimbursement_system.claim;

import com.Urvashi.expense_reimbursement_system.claim.dto.ClaimDecisionRequest;
import com.Urvashi.expense_reimbursement_system.claim.dto.ClaimRequest;
import com.Urvashi.expense_reimbursement_system.claim.dto.ClaimResponse;
import com.Urvashi.expense_reimbursement_system.claim.dto.StatusHistoryResponse;
import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClaimController {

    private static final String OWN_CLAIMS_ROLES = "hasAnyRole('EMPLOYEE','MANAGER','FINANCE')";

    private final ClaimService claimService;

    // ---------------- own claims (EMPLOYEE, MANAGER, FINANCE) ----------------

    @PreAuthorize(OWN_CLAIMS_ROLES)
    @PostMapping("/claims")
    public ResponseEntity<ClaimResponse> createDraft(@Valid @RequestBody ClaimRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(claimService.createDraft(request, authentication.getName()));
    }

    @PreAuthorize(OWN_CLAIMS_ROLES)
    @GetMapping("/claims")
    public PageResponse<ClaimResponse> listMyClaims(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return claimService.listMyClaims(authentication.getName(), status, pageable);
    }

    @PreAuthorize(OWN_CLAIMS_ROLES)
    @PutMapping("/claims/{id}")
    public ClaimResponse updateClaim(@PathVariable Long id,
                                     @Valid @RequestBody ClaimRequest request,
                                     Authentication authentication) {
        return claimService.updateClaim(id, request, authentication.getName());
    }

    @PreAuthorize(OWN_CLAIMS_ROLES)
    @PostMapping("/claims/{id}/submit")
    public ClaimResponse submitClaim(@PathVariable Long id, Authentication authentication) {
        return claimService.submitClaim(id, authentication.getName());
    }

    @PreAuthorize(OWN_CLAIMS_ROLES)
    @PostMapping("/claims/{id}/withdraw")
    public ClaimResponse withdrawClaim(@PathVariable Long id, Authentication authentication) {
        return claimService.withdrawClaim(id, authentication.getName());
    }

    // ---------------- viewing one claim: any logged-in user, the service decides ----------------

    @GetMapping("/claims/{id}")
    public ClaimResponse getClaim(@PathVariable Long id, Authentication authentication) {
        return claimService.getClaim(id, authentication.getName());
    }

    @GetMapping("/claims/{id}/history")
    public List<StatusHistoryResponse> getHistory(@PathVariable Long id, Authentication authentication) {
        return claimService.getHistory(id, authentication.getName());
    }

    // ---------------- manager ----------------

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager/claims")
    public PageResponse<ClaimResponse> listManagerClaims(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication) {
        return claimService.listManagerClaims(authentication.getName(), status, pageable);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/manager/claims/{id}/approve")
    public ClaimResponse approveClaim(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) ClaimDecisionRequest request,
                                      Authentication authentication) {
        String comment = (request == null) ? null : request.comment();
        return claimService.approveClaim(id, comment, authentication.getName());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/manager/claims/{id}/reject")
    public ClaimResponse rejectClaim(@PathVariable Long id,
                                     @Valid @RequestBody(required = false) ClaimDecisionRequest request,
                                     Authentication authentication) {
        String comment = (request == null) ? null : request.comment();
        return claimService.rejectClaim(id, comment, authentication.getName());
    }

    // ---------------- finance (the reimbursement queue; paying comes in a later step) ----------------

    @PreAuthorize("hasRole('FINANCE')")
    @GetMapping("/finance/claims")
    public PageResponse<ClaimResponse> listFinanceClaims(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return claimService.listFinanceClaims(status, pageable);
    }

    // ---------------- admin ----------------

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/claims")
    public PageResponse<ClaimResponse> listAllClaims(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return claimService.listAllClaims(status, pageable);
    }
}