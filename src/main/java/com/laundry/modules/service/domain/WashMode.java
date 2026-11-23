package com.laundry.modules.service.domain;

import com.laundry.common.entity.BaseEntity;
import java.util.Comparator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Unique theo lower(name) do script tạo (uk_wash_modes_name_ci); legacy_order_type và sort_order (bỏ dùng, DEFAULT 0) không map.
@Entity
@Table(name = "wash_modes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WashMode extends BaseEntity {

    /** Thứ tự hiển thị mọi nơi (FR-034): mặc định trước, giờ hoàn thành giảm dần, lower(name), id. */
    public static final Comparator<WashMode> DISPLAY_ORDER = Comparator
            .comparing((WashMode m) -> !Boolean.TRUE.equals(m.getIsDefault()))
            .thenComparing(WashMode::getCompletionHours, Comparator.reverseOrder())
            .thenComparing(m -> m.getName().toLowerCase())
            .thenComparing(WashMode::getId);

    // JPA nullable: ddl-auto chạy trước migrate script (backfill + NOT NULL + unique do script); không có setter từ request
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "completion_hours", nullable = false)
    private Integer completionHours;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
}
