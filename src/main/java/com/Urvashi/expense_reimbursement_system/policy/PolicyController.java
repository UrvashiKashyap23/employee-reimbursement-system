package com.Urvashi.expense_reimbursement_system.policy;

import com.Urvashi.expense_reimbursement_system.policy.dto.CategoryRequest;
import com.Urvashi.expense_reimbursement_system.policy.dto.CategoryResponse;
import com.Urvashi.expense_reimbursement_system.policy.dto.PolicyRequest;
import com.Urvashi.expense_reimbursement_system.policy.dto.PolicyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // Any logged-in user: used by the claim form to show categories and rules
    @GetMapping("/categories")
    public List<CategoryResponse> listActiveCategories() {
        return policyService.listActiveCategories();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/categories")
    public List<CategoryResponse> listAllCategories() {
        return policyService.listAllCategories();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyService.createCategory(request, authentication.getName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id,
                                           @Valid @RequestBody CategoryRequest request,
                                           Authentication authentication) {
        return policyService.updateCategory(id, request, authentication.getName());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/categories/{categoryId}/policy")
    public PolicyResponse getPolicy(@PathVariable Long categoryId) {
        return policyService.getPolicy(categoryId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/categories/{categoryId}/policy")
    public PolicyResponse upsertPolicy(@PathVariable Long categoryId,
                                       @Valid @RequestBody PolicyRequest request,
                                       Authentication authentication) {
        return policyService.upsertPolicy(categoryId, request, authentication.getName());
    }
}