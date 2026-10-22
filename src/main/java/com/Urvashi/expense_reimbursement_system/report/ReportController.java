package com.Urvashi.expense_reimbursement_system.report;

import com.Urvashi.expense_reimbursement_system.report.dto.CategoryTotal;
import com.Urvashi.expense_reimbursement_system.report.dto.MonthlyTotal;
import com.Urvashi.expense_reimbursement_system.report.dto.SummaryReport;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','FINANCE','ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // GET /api/reports/summary?from=2026-10-01&to=2026-10-31   (both dates optional)
    @GetMapping("/summary")
    public SummaryReport summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return reportService.summary(authentication.getName(), from, to);
    }

    @GetMapping("/monthly")
    public List<MonthlyTotal> monthly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return reportService.monthlyTotals(authentication.getName(), from, to);
    }

    @GetMapping("/by-category")
    public List<CategoryTotal> byCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return reportService.categoryTotals(authentication.getName(), from, to);
    }
}