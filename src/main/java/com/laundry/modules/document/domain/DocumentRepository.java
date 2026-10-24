package com.laundry.modules.document.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByStorageKey(String storageKey);
}
