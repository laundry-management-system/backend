package com.laundry.modules.order.domain;

/** Số đơn Mới nhận + Đang xử lý theo từng tab của Danh sách công việc. */
public interface WorkCounts {
    long getTotal();

    long getUnclassified();

    long getReceived();

    long getProcessing();

    long getLate();

    long getExpress();

    long getPendingConfirmation();

    long getAwaitingDropoff();
}
