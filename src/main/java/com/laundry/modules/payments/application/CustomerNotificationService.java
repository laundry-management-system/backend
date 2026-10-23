package com.laundry.modules.payments.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.payments.application.dto.ComposeNotificationRequest;
import com.laundry.modules.payments.application.dto.NotificationResponse;
import com.laundry.modules.payments.application.port.OrderIntegration;
import com.laundry.modules.payments.application.port.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    private final OrderIntegration orderIntegration;

    @Value("${app.frontend.public-lookup-url:http://localhost:5173/track-order}")
    private String publicLookupUrl;

    @Transactional
    public NotificationResponse compose(String orderCode, ComposeNotificationRequest.NotificationChannel channel) {
        OrderSnapshot order = orderIntegration.lockByCode(orderCode);
        if (!"COMPLETED".equals(order.status())) {
            throw new BusinessException("Chỉ có thể nhắn khách về đơn đã hoàn thành");
        }

        String lookupLink = UriComponentsBuilder.fromUriString(publicLookupUrl)
                .queryParam("orderCode", order.orderCode())
                .build().encode().toUriString();
        String message = "Đơn giặt " + order.orderCode() + " đã hoàn thành. Ngày hẹn trả: " + order.dueAt()
                + ". Tra cứu: " + lookupLink;
        String deepLink = switch (channel) {
            case SMS -> UriComponentsBuilder.fromUriString("sms:" + order.customerPhone())
                    .queryParam("body", message).build().encode().toUriString();
            case ZALO -> "https://zalo.me/" + order.customerPhone();
        };

        Instant remindedAt = Instant.now();
        orderIntegration.recordReminder(order.id(), remindedAt);
        OrderSnapshot updated = orderIntegration.findByCode(orderCode)
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", orderCode));
        return new NotificationResponse(order.orderCode(), channel, message, deepLink, lookupLink,
                updated.lastNotifiedAt(), updated.notifyCount());
    }
}
