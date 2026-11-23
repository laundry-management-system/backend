package com.laundry.modules.payments.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderIntegration {

    Optional<OrderSnapshot> findByCode(String orderCode);

    /** ONLINE order whose return leg is DELIVERY. */
    boolean isOnlineDelivery(Long orderId);

    OrderSnapshot lockByCode(String orderCode);

    void setPaymentStatus(long orderId, boolean paid);

    void deliver(long orderId, Instant deliveredAt, String deliveredBy, String receiverName, String receiverRelation);

    void recordReminder(long orderId, Instant remindedAt);

    void appendNote(long orderId, String note);

    List<OverdueSnapshot> findOverdue(Instant completedBefore);
}
