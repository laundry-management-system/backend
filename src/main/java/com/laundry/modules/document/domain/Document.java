package com.laundry.modules.document.domain;

import com.laundry.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** File đã tải lên (ảnh, tài liệu). Các module khác tham chiếu qua id, không tự lưu file. */
@Entity
@Table(name = "documents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Document extends BaseEntity {

    /** Tên file trên đĩa (UUID + đuôi): không đoán được, không dùng tên gốc để tránh path traversal */
    @Column(name = "storage_key", nullable = false, unique = true, length = 64)
    private String storageKey;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "uploaded_by", length = 50)
    private String uploadedBy;

    public boolean isImage() {
        return contentType.startsWith("image/");
    }
}
