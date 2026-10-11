package com.Urvashi.expense_reimbursement_system.user;

import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.common.exception.ResourceNotFoundException;
import com.Urvashi.expense_reimbursement_system.user.dto.AdminUpdateUserRequest;
import com.Urvashi.expense_reimbursement_system.user.dto.UpdateProfileRequest;
import com.Urvashi.expense_reimbursement_system.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ---------- helpers other modules will reuse ----------

    @Transactional(readOnly = true)
    public User getEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    // ---------- employee: own profile ----------

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return UserResponse.from(getEntityByEmail(email));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getEntityByEmail(email);
        user.setFullName(request.fullName().trim());
        // No save() needed: inside a transaction Hibernate notices the change
        // ("dirty checking") and runs the UPDATE when the transaction commits.
        return UserResponse.from(user);
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(UserResponse::from));
    }

    @Transactional
    public UserResponse adminUpdateUser(Long targetId, AdminUpdateUserRequest request, String actingAdminEmail) {
        User target = getEntityById(targetId);
        User admin = getEntityByEmail(actingAdminEmail);

        if (target.getId().equals(admin.getId())) {
            throw new BusinessRuleException("You cannot change your own role, manager or active status");
        }

        User manager = null;
        if (request.managerId() != null) {
            if (request.managerId().equals(target.getId())) {
                throw new BusinessRuleException("A user cannot be their own manager");
            }
            manager = getEntityById(request.managerId());
            if (manager.getRole() != Role.MANAGER) {
                throw new BusinessRuleException("The selected manager must have the MANAGER role");
            }
            if (!manager.isActive()) {
                throw new BusinessRuleException("The selected manager is not active");
            }
        }

        target.setRole(request.role());
        target.setManager(manager);
        target.setActive(request.active());
        return UserResponse.from(target);
    }
}