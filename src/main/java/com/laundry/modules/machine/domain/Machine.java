package com.laundry.modules.machine.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Máy giặt / máy sấy (BM8 / QĐ10, BR-E01, BR-E02). */
@Entity
@Table(
        name = "machines",
        indexes = {
                @Index(name = "idx_machines_type", columnList = "type"),
                @Index(name = "idx_machines_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Machine extends BaseEntity {

    @Column(name = "machine_code", nullable = false, unique = true, length = 10)
    private String machineCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MachineType type;

    @Column(name = "capacity_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal capacityKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private MachineStatus status = MachineStatus.READY;

    @Column(length = 500)
    private String note;

    /** NV và Admin chỉ chuyển Sẵn sàng ⇄ Bảo trì; máy đã ngừng sử dụng không đổi được nữa. */
    public void changeOperationalStatus(MachineStatus target, String reason) {
        if (status == MachineStatus.RETIRED) {
            throw new BusinessException("Máy đã ngừng sử dụng, không đổi được trạng thái");
        }
        if (target == MachineStatus.RETIRED) {
            throw new BusinessException("Dùng chức năng Ngừng sử dụng (chỉ Admin) để cho máy ngừng hoạt động", "status");
        }
        if (target == status) {
            throw new BusinessException("Máy đã ở trạng thái " + target.getDescription(), "status");
        }
        if (target == MachineStatus.MAINTENANCE && (reason == null || reason.isBlank())) {
            throw new BusinessException("Bắt buộc nhập lý do khi chuyển máy sang Bảo trì", "reason");
        }
        this.status = target;
    }

    public void retire() {
        if (status == MachineStatus.RETIRED) {
            throw new BusinessException("Máy đã ngừng sử dụng trước đó");
        }
        this.status = MachineStatus.RETIRED;
    }
}
