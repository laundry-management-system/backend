package com.laundry.modules.document.application.dto;

import com.laundry.modules.document.domain.Document;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String url,
        String originalName,
        String contentType,
        Long sizeBytes,
        Instant createdAt
) {
    public static String urlOf(Document document) {
        return "/api/documents/files/" + document.getStorageKey();
    }

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), urlOf(d), d.getOriginalName(), d.getContentType(), d.getSizeBytes(), d.getCreatedAt());
    }
}
