package com.laundry.modules.incident.domain;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    List<Incident> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    static Specification<Incident> hasStatus(IncidentStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Incident> hasType(IncidentType type) {
        return type == null ? null : (root, q, cb) -> cb.equal(root.get("type"), type);
    }

    static Specification<Incident> createdFrom(Instant from) {
        return from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    static Specification<Incident> createdBefore(Instant to) {
        return to == null ? null : (root, q, cb) -> cb.lessThan(root.get("createdAt"), to);
    }
}
