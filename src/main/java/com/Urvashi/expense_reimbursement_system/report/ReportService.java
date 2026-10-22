package com.Urvashi.expense_reimbursement_system.report;

import com.Urvashi.expense_reimbursement_system.claim.ClaimStatus;
import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.report.dto.CategoryTotal;
import com.Urvashi.expense_reimbursement_system.report.dto.MonthlyTotal;
import com.Urvashi.expense_reimbursement_system.report.dto.SummaryReport;
import com.Urvashi.expense_reimbursement_system.user.Role;
import com.Urvashi.expense_reimbursement_system.user.User;
import com.Urvashi.expense_reimbursement_system.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String CURRENCY = "INR";
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    // Claims that count as real spend: not drafts, not rejected, not withdrawn
    private static final List<ClaimStatus> SPEND_STATUSES =
            List.of(ClaimStatus.SUBMITTED, ClaimStatus.MANAGER_APPROVED, ClaimStatus.REIMBURSED);

    private final ReportRepository reportRepository;
    private final UserService userService;

    // Whose claims a user's numbers are calculated from. Decided here on the server, never by the client.
    private record Scope(String name, Long employeeId, Long managerId) {
    }

    private Scope scopeFor(User user) {
        return switch (user.getRole()) {
            case EMPLOYEE -> new Scope("OWN", user.getId(), null);
            case MANAGER -> new Scope("TEAM", null, user.getId());
            case FINANCE, ADMIN -> new Scope("ALL", null, null);
        };
    }

    @Transactional(readOnly = true)
    public SummaryReport summary(String email, LocalDate from, LocalDate to) {
        validateRange(from, to);
        User user = userService.getEntityByEmail(email);
        Scope scope = scopeFor(user);

        Map<ClaimStatus, Long> counts = new EnumMap<>(ClaimStatus.class);
        Map<ClaimStatus, BigDecimal> sums = new EnumMap<>(ClaimStatus.class);
        for (Object[] row : reportRepository.countAndSumByStatus(scope.employeeId(), scope.managerId(), from, to)) {
            ClaimStatus status = (ClaimStatus) row[0];
            counts.put(status, (Long) row[1]);
            sums.put(status, (BigDecimal) row[2]);
        }

        long totalSubmitted = count(counts, ClaimStatus.SUBMITTED)
                + count(counts, ClaimStatus.MANAGER_APPROVED)
                + count(counts, ClaimStatus.REJECTED)
                + count(counts, ClaimStatus.REIMBURSED);

        // Employees cannot see review flags, so they do not get this number
        Long openFlaggedClaims = (user.getRole() == Role.EMPLOYEE)
                ? null
                : reportRepository.countClaimsWithOpenFlags(scope.employeeId(), scope.managerId(), from, to);

        return new SummaryReport(
                scope.name(),
                from,
                to,
                CURRENCY,
                totalSubmitted,
                count(counts, ClaimStatus.SUBMITTED),
                count(counts, ClaimStatus.MANAGER_APPROVED),
                sum(sums, ClaimStatus.MANAGER_APPROVED),
                count(counts, ClaimStatus.REJECTED),
                count(counts, ClaimStatus.REIMBURSED),
                sum(sums, ClaimStatus.REIMBURSED),
                openFlaggedClaims
        );
    }

    @Transactional(readOnly = true)
    public List<MonthlyTotal> monthlyTotals(String email, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Scope scope = scopeFor(userService.getEntityByEmail(email));

        return reportRepository.monthlyTotals(scope.employeeId(), scope.managerId(), from, to, SPEND_STATUSES)
                .stream()
                .map(row -> new MonthlyTotal(
                        String.format("%04d-%02d", ((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        ((Number) row[2]).longValue(),
                        (BigDecimal) row[3]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryTotal> categoryTotals(String email, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Scope scope = scopeFor(userService.getEntityByEmail(email));

        return reportRepository.categoryTotals(scope.employeeId(), scope.managerId(), from, to, SPEND_STATUSES)
                .stream()
                .map(row -> new CategoryTotal(
                        (Long) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        (BigDecimal) row[3]))
                .toList();
    }

    // ---------- private ----------

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("'from' date must not be after 'to' date");
        }
    }

    private long count(Map<ClaimStatus, Long> counts, ClaimStatus status) {
        return counts.getOrDefault(status, 0L);
    }

    private BigDecimal sum(Map<ClaimStatus, BigDecimal> sums, ClaimStatus status) {
        return sums.getOrDefault(status, ZERO);
    }
}