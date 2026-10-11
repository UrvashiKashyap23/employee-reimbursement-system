package com.Urvashi.expense_reimbursement_system.user;

import com.Urvashi.expense_reimbursement_system.common.dto.PageResponse;
import com.Urvashi.expense_reimbursement_system.user.dto.AdminUpdateUserRequest;
import com.Urvashi.expense_reimbursement_system.user.dto.UpdateProfileRequest;
import com.Urvashi.expense_reimbursement_system.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/me")
    public UserResponse getMyProfile(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @PutMapping("/users/me")
    public UserResponse updateMyProfile(Authentication authentication,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication.getName(), request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public PageResponse<UserResponse> listUsers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/users/{id}")
    public UserResponse adminUpdateUser(@PathVariable Long id,
                                        @Valid @RequestBody AdminUpdateUserRequest request,
                                        Authentication authentication) {
        return userService.adminUpdateUser(id, request, authentication.getName());
    }
}