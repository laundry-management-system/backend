package com.laundry.modules.log.application.dto;

import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import java.util.Map;

/**
 * Một dòng nhật ký cần ghi, do module nghiệp vụ truyền cho AuditService.
 * Người thực hiện, IP và thời điểm do AuditService tự điền, nơi gọi không truyền.
 *
 * @param action     loại hành động (ví dụ ACCOUNT_MANAGE, PRICE_CHANGE)
 * @param entityType loại đối tượng bị tác động (order, user, service, payment, machine...)
 * @param entityId   id của đối tượng bị tác động
 * @param oldData    dữ liệu trước khi đổi (dev tự chọn các field cần lưu và tạo thành map với key:value = tên biến: giá trị); null nếu là tạo mới
 * @param newData    dữ liệu sau khi đổi (dev tự chọn các field cần lưu và tạo thành map với key:value = tên biến: giá trị); null nếu là xóa
 * @param reason     lý do; bắt buộc với lùi trạng thái, dời hẹn, hủy thanh toán, còn lại có thể null
 */
public record AuditEntry(
        AuditAction action,
        EntityType entityType,
        Long entityId,
        Map<String, Object> oldData,
        Map<String, Object> newData,
        String reason
) {}
