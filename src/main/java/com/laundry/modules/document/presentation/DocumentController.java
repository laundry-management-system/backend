package com.laundry.modules.document.presentation;

import com.laundry.modules.document.application.DocumentService;
import com.laundry.modules.document.application.dto.DocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@Tag(name = "Documents", description = "Tải lên và đọc file (ảnh, tài liệu) dùng chung cho các module")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload a file (PNG/JPG/WEBP/PDF, max 5MB)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        return documentService.upload(file, authentication != null ? authentication.getName() : null);
    }

    // Public: thẻ <img> không gửi được JWT; storageKey là UUID nên không đoán được
    @Operation(summary = "Read file content by storage key (public)")
    @GetMapping("/files/{storageKey}")
    public ResponseEntity<Resource> file(@PathVariable String storageKey) {
        DocumentService.StoredFile stored = documentService.getContent(storageKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(stored.resource());
    }
}
