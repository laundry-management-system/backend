package com.laundry.modules.document.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.document.application.dto.DocumentResponse;
import com.laundry.modules.document.domain.Document;
import com.laundry.modules.document.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    /** Chỉ nhận định dạng an toàn để phục vụ công khai (không HTML/SVG); content-type → đuôi file */
    static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "application/pdf", "pdf"
    );
    static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final DocumentRepository documentRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public DocumentResponse upload(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn file", "file");
        }
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException("Chỉ nhận ảnh PNG, JPG, WEBP hoặc file PDF", "file");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("File tối đa 5MB", "file");
        }

        String storageKey = UUID.randomUUID() + "." + extension;
        try {
            Path dir = Path.of(uploadDir);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(storageKey).toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException("Không lưu được file", e);
        }

        Document saved = documentRepository.save(Document.builder()
                .storageKey(storageKey)
                .originalName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .uploadedBy(username)
                .build());
        return DocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public StoredFile getContent(String storageKey) {
        Document document = documentRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy file"));
        Resource resource = new FileSystemResource(Path.of(uploadDir).resolve(document.getStorageKey()));
        if (!resource.exists()) {
            throw new NotFoundException("Không tìm thấy file");
        }
        return new StoredFile(resource, document.getContentType());
    }

    /** Cho module khác gắn ảnh: document phải tồn tại và là ảnh. */
    @Transactional(readOnly = true)
    public Document requireImage(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("File", id));
        if (!document.isImage()) {
            throw new BusinessException("File đính kèm phải là ảnh", "documentId");
        }
        return document;
    }

    public record StoredFile(Resource resource, String contentType) {
    }
}
