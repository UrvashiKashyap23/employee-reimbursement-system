package com.Urvashi.expense_reimbursement_system.report;

import com.Urvashi.expense_reimbursement_system.claim.ClaimStatus;
import com.Urvashi.expense_reimbursement_system.fraud.FlagStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // One row per status: [ClaimStatus, Long count, BigDecimal totalAmount]
    @SuppressWarnings("unchecked")
    public List<Object[]> countAndSumByStatus(Long employeeId, Long managerId, LocalDate from, LocalDate to) {
        String jpql = "select c.status, count(c), sum(c.amount) from ExpenseClaim c where 1 = 1"
                + filters("c", employeeId, managerId, from, to)
                + " group by c.status";
        Query query = entityManager.createQuery(jpql);
        bind(query, employeeId, managerId, from, to);
        return query.getResultList();
    }

    // One row per month: [Integer year, Integer month, Long count, BigDecimal totalAmount]
    @SuppressWarnings("unchecked")
    public List<Object[]> monthlyTotals(Long employeeId, Long managerId, LocalDate from, LocalDate to,
                                        List<ClaimStatus> statuses) {
        String jpql = "select extract(year from c.expenseDate), extract(month from c.expenseDate), "
                + "count(c), sum(c.amount) from ExpenseClaim c where c.status in :statuses"
                + filters("c", employeeId, managerId, from, to)
                + " group by extract(year from c.expenseDate), extract(month from c.expenseDate)"
                + " order by extract(year from c.expenseDate), extract(month from c.expenseDate)";
        Query query = entityManager.createQuery(jpql);
        query.setParameter("statuses", statuses);
        bind(query, employeeId, managerId, from, to);
        return query.getResultList();
    }

    // One row per category: [Long categoryId, String name, Long count, BigDecimal totalAmount]
    @SuppressWarnings("unchecked")
    public List<Object[]> categoryTotals(Long employeeId, Long managerId, LocalDate from, LocalDate to,
                                         List<ClaimStatus> statuses) {
        String jpql = "select cat.id, cat.name, count(c), sum(c.amount) "
                + "from ExpenseClaim c join c.category cat where c.status in :statuses"
                + filters("c", employeeId, managerId, from, to)
                + " group by cat.id, cat.name order by sum(c.amount) desc";
        Query query = entityManager.createQuery(jpql);
        query.setParameter("statuses", statuses);
        bind(query, employeeId, managerId, from, to);
        return query.getResultList();
    }

    // How many different claims still have at least one OPEN flag
    public long countClaimsWithOpenFlags(Long employeeId, Long managerId, LocalDate from, LocalDate to) {
        String jpql = "select count(distinct f.claim.id) from ClaimFlag f where f.status = :openStatus"
                + filters("f.claim", employeeId, managerId, from, to);
        Query query = entityManager.createQuery(jpql);
        query.setParameter("openStatus", FlagStatus.OPEN);
        bind(query, employeeId, managerId, from, to);
        return ((Number) query.getSingleResult()).longValue();
    }

    // ---------- private ----------

    // Adds a condition only when that filter is actually set
    private String filters(String claimAlias, Long employeeId, Long managerId, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        if (employeeId != null) {
            sb.append(" and ").append(claimAlias).append(".employee.id = :employeeId");
        }
        if (managerId != null) {
            sb.append(" and ").append(claimAlias).append(".assignedManager.id = :managerId");
        }
        if (from != null) {
            sb.append(" and ").append(claimAlias).append(".expenseDate >= :fromDate");
        }
        if (to != null) {
            sb.append(" and ").append(claimAlias).append(".expenseDate <= :toDate");
        }
        return sb.toString();
    }

    private void bind(Query query, Long employeeId, Long managerId, LocalDate from, LocalDate to) {
        if (employeeId != null) {
            query.setParameter("employeeId", employeeId);
        }
        if (managerId != null) {
            query.setParameter("managerId", managerId);
        }
        if (from != null) {
            query.setParameter("fromDate", from);
        }
        if (to != null) {
            query.setParameter("toDate", to);
        }
    }
}