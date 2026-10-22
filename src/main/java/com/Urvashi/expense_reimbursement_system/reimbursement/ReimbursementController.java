package com.Urvashi.expense_reimbursement_system.reimbursement;

import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import com.Urvashi.expense_reimbursement_system.reimbursement.dto.ReimbursementRequest;
import com.Urvashi.expense_reimbursement_system.reimbursement.dto.ReimbursementResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    @PreAuthorize("hasRole('FINANCE')")
    @PostMapping("/finance/claims/{claimId}/reimburse")
    public ResponseEntity<ReimbursementResponse> reimburse(@PathVariable Long claimId,
                                                           @Valid @RequestBody ReimbursementRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reimbursementService.reimburse(claimId, request, authentication.getName()));
    }

    // GET /api/reimbursements?from=2026-10-01&to=2026-10-31&page=0&size=20
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    @GetMapping("/reimbursements")
    public PageResponse<ReimbursementResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reimbursementService.list(from, to, pageable);
    }

    // Any logged-in user; the service checks that they may see this claim
    @GetMapping("/claims/{claimId}/reimbursement")
    public ReimbursementResponse getForClaim(@PathVariable Long claimId, Authentication authentication) {
        return reimbursementService.getForClaim(claimId, authentication.getName());
    }
}