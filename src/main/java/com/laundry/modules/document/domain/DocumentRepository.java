package com.laundry.modules.document.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByStorageKey(String storageKey);
}
