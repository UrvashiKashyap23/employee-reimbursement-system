package com.Urvashi.expense_reimbursement_system.policy;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditService;
import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.common.exception.ResourceNotFoundException;
import com.Urvashi.expense_reimbursement_system.policy.dto.CategoryRequest;
import com.Urvashi.expense_reimbursement_system.policy.dto.CategoryResponse;
import com.Urvashi.expense_reimbursement_system.policy.dto.PolicyRequest;
import com.Urvashi.expense_reimbursement_system.policy.dto.PolicyResponse;
import com.Urvashi.expense_reimbursement_system.user.User;
import com.Urvashi.expense_reimbursement_system.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpensePolicyRepository policyRepository;
    private final UserService userService;
    private final AuditService auditService;

    // ---------- reading ----------

    // For every logged-in user: only active categories, with their active policy (if any)
    @Transactional(readOnly = true)
    public List<CategoryResponse> listActiveCategories() {
        Map<Long, ExpensePolicy> policies = policiesByCategoryId();
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(category -> {
                    ExpensePolicy policy = policies.get(category.getId());
                    boolean showPolicy = policy != null && policy.isActive();
                    return toResponse(category, showPolicy ? policy : null);
                })
                .toList();
    }

    // For admins: everything, including inactive categories and policies
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAllCategories() {
        Map<Long, ExpensePolicy> policies = policiesByCategoryId();
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> toResponse(category, policies.get(category.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(Long categoryId) {
        getCategoryEntity(categoryId);
        ExpensePolicy policy = policyRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("No policy defined for category id: " + categoryId));
        return PolicyResponse.from(policy);
    }

    // ---------- admin: categories ----------

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, String adminEmail) {
        User admin = userService.getEntityByEmail(adminEmail);
        String name = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("A category with this name already exists", HttpStatus.CONFLICT);
        }

        ExpenseCategory category = new ExpenseCategory();
        category.setName(name);
        category.setActive(request.active() == null || request.active());
        ExpenseCategory saved = categoryRepository.save(category);

        auditService.record(admin, AuditAction.CATEGORY_CREATED, "CATEGORY", saved.getId(),
                "Created category '" + name + "'");
        return toResponse(saved, null);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, String adminEmail) {
        User admin = userService.getEntityByEmail(adminEmail);
        ExpenseCategory category = getCategoryEntity(id);
        String name = request.name().trim();

        categoryRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessRuleException("A category with this name already exists", HttpStatus.CONFLICT);
                });

        category.setName(name);
        if (request.active() != null) {
            category.setActive(request.active());
        }

        auditService.record(admin, AuditAction.CATEGORY_UPDATED, "CATEGORY", category.getId(),
                "Category updated: name='" + category.getName() + "', active=" + category.isActive());
        return toResponse(category, policyRepository.findByCategoryId(id).orElse(null));
    }

    // ---------- admin: policies ----------

    // Creates the policy if the category has none, otherwise updates it
    @Transactional
    public PolicyResponse upsertPolicy(Long categoryId, PolicyRequest request, String adminEmail) {
        User admin = userService.getEntityByEmail(adminEmail);
        ExpenseCategory category = getCategoryEntity(categoryId);

        Optional<ExpensePolicy> existing = policyRepository.findByCategoryId(categoryId);
        boolean isNew = existing.isEmpty();
        ExpensePolicy policy = existing.orElseGet(ExpensePolicy::new);

        if (isNew) {
            policy.setCategory(category);
        }
        policy.setMaxAmount(request.maxAmount());
        policy.setReceiptRequired(request.receiptRequired());
        policy.setMonthlyClaimLimit(request.monthlyClaimLimit());
        if (request.active() != null) {
            policy.setActive(request.active());
        }
        policy.setUpdatedBy(admin);
        ExpensePolicy saved = policyRepository.save(policy);

        auditService.record(admin, isNew ? AuditAction.POLICY_CREATED : AuditAction.POLICY_UPDATED,
                "POLICY", saved.getId(),
                "Category '" + category.getName() + "': maxAmount=" + saved.getMaxAmount()
                        + ", receiptRequired=" + saved.isReceiptRequired()
                        + ", monthlyClaimLimit=" + saved.getMonthlyClaimLimit()
                        + ", active=" + saved.isActive());
        return PolicyResponse.from(saved);
    }

    // ---------- helpers other modules will reuse (the claim module) ----------

    @Transactional(readOnly = true)
    public ExpenseCategory getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Transactional(readOnly = true)
    public Optional<ExpensePolicy> findActivePolicy(Long categoryId) {
        return policyRepository.findByCategoryId(categoryId).filter(ExpensePolicy::isActive);
    }

    // ---------- private ----------

    private Map<Long, ExpensePolicy> policiesByCategoryId() {
        return policyRepository.findAll().stream()
                .collect(Collectors.toMap(policy -> policy.getCategory().getId(), Function.identity()));
    }

    private CategoryResponse toResponse(ExpenseCategory category, ExpensePolicy policy) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.isActive(),
                policy != null ? PolicyResponse.from(policy) : null
        );
    }
}