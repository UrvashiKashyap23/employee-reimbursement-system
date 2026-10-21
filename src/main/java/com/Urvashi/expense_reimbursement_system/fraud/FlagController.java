package com.Urvashi.expense_reimbursement_system.fraud;

import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import com.Urvashi.expense_reimbursement_system.fraud.dto.FlagResponse;
import com.Urvashi.expense_reimbursement_system.fraud.dto.ResolveFlagRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FlagController {

    private final FlagService flagService;

    // Flags on one claim: reviewers only (the service blocks the claim owner)
    @PreAuthorize("hasAnyRole('MANAGER','FINANCE','ADMIN')")
    @GetMapping("/claims/{claimId}/flags")
    public List<FlagResponse> listForClaim(@PathVariable Long claimId, Authentication authentication) {
        return flagService.listForClaim(claimId, authentication.getName());
    }

    // The manager's queue of flags on claims assigned to them
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager/flags")
    public PageResponse<FlagResponse> listManagerFlags(
            @RequestParam(required = false) FlagStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return flagService.listManagerFlags(authentication.getName(), status, pageable);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/manager/flags/{id}/resolve")
    public FlagResponse resolve(@PathVariable Long id,
                                @Valid @RequestBody ResolveFlagRequest request,
                                Authentication authentication) {
        return flagService.resolve(id, request, authentication.getName());
    }

    // Admin: flagged-claim information across the whole system (view only)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/flags")
    public PageResponse<FlagResponse> listAllFlags(
            @RequestParam(required = false) FlagStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return flagService.listAllFlags(status, pageable);
    }
}