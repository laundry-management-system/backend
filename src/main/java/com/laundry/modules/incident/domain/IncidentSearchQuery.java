package com.laundry.modules.incident.domain;

import com.laundry.modules.order.domain.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** Danh sách và thống kê dùng chung một bộ lọc để số đếm trên tab luôn khớp danh sách. */
@Repository
@RequiredArgsConstructor
public class IncidentSearchQuery {

    private final EntityManager entityManager;

    public record Stats(Map<IncidentStatus, Long> counts, BigDecimal paidCash, BigDecimal paidTransfer) {
    }

    public Page<Incident> search(IncidentFilter filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Incident> cq = cb.createQuery(Incident.class);
        Root<Incident> root = cq.from(Incident.class);
        cq.select(root).where(predicates(cb, root, filter, true)).orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
        List<Incident> content = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Incident> countRoot = countQuery.from(Incident.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countRoot, filter, true));
        long total = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    public Stats stats(IncidentFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> byStatus = cb.createQuery(Object[].class);
        Root<Incident> root = byStatus.from(Incident.class);
        byStatus.multiselect(root.get("status"), cb.count(root))
                .where(predicates(cb, root, filter, false))
                .groupBy(root.get("status"));
        Map<IncidentStatus, Long> counts = new EnumMap<>(IncidentStatus.class);
        for (IncidentStatus s : IncidentStatus.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : entityManager.createQuery(byStatus).getResultList()) {
            counts.put((IncidentStatus) row[0], (Long) row[1]);
        }

        CriteriaQuery<Object[]> paid = cb.createQuery(Object[].class);
        Root<Incident> paidRoot = paid.from(Incident.class);
        List<Predicate> paidWhere = new ArrayList<>(List.of(predicates(cb, paidRoot, filter, false)));
        paidWhere.add(cb.isNotNull(paidRoot.get("paidAt")));
        paid.multiselect(paidRoot.get("payoutMethod"), cb.sum(paidRoot.<BigDecimal>get("payoutAmount")))
                .where(paidWhere.toArray(Predicate[]::new))
                .groupBy(paidRoot.get("payoutMethod"));
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal transfer = BigDecimal.ZERO;
        for (Object[] row : entityManager.createQuery(paid).getResultList()) {
            BigDecimal sum = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            if (row[0] == PayoutMethod.CASH) {
                cash = sum;
            } else if (row[0] == PayoutMethod.BANK_TRANSFER) {
                transfer = sum;
            }
        }
        return new Stats(counts, cash, transfer);
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<Incident> root, IncidentFilter f, boolean withStatus) {
        List<Predicate> where = new ArrayList<>();
        Join<Incident, Order> order = root.join("order");
        if (withStatus && f.status() != null) {
            where.add(cb.equal(root.get("status"), f.status()));
        }
        if (f.type() != null) {
            where.add(cb.equal(root.get("type"), f.type()));
        }
        if (f.orderId() != null) {
            where.add(cb.equal(order.get("id"), f.orderId()));
        }
        if (f.createdFrom() != null) {
            where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.createdFrom()));
        }
        if (f.createdTo() != null) {
            where.add(cb.lessThan(root.get("createdAt"), f.createdTo()));
        }
        if (f.q() != null && !f.q().isBlank()) {
            String term = f.q().trim().toLowerCase(Locale.ROOT);
            String like = "%" + term + "%";
            String digits = term.replaceAll("\\D", "");
            List<Predicate> any = new ArrayList<>();
            any.add(cb.like(cb.lower(root.get("code")), like));
            any.add(cb.like(cb.lower(order.get("orderCode")), like));
            if (!digits.isEmpty()) {
                any.add(cb.like(order.get("customerPhone"), "%" + digits + "%"));
            }
            where.add(cb.or(any.toArray(Predicate[]::new)));
        }
        return where.toArray(Predicate[]::new);
    }
}
