package com.laundry.modules.payments.application.port;

/** Ca làm việc của module shift, đọc qua bảng chung để không import module đó. */
public interface ShiftIntegration {

    /**
     * Khóa ca đang mở (FOR UPDATE) tới hết transaction và trả id.
     * Không có ca mở → ResourceConflictException (409).
     */
    long lockOpenShiftId();
}
