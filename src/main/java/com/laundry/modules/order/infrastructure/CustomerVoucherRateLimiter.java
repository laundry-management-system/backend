package com.laundry.modules.order.infrastructure;

import com.laundry.common.exception.TooManyRequestsException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Giới hạn số lần khách thử mã voucher (FR-045); mọi lần thử đều tính, kể cả thất bại. */
@Component
public class CustomerVoucherRateLimiter {

    // shortcut: in-memory per instance, move to Redis if the BE is scaled out
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxPerCustomer;
    private final int maxPerIp;
    private final long windowMillis;

    public CustomerVoucherRateLimiter(
            @Value("${app.customer-voucher.max-per-customer:5}") int maxPerCustomer,
            @Value("${app.customer-voucher.max-per-ip:20}") int maxPerIp,
            @Value("${app.customer-voucher.window-seconds:600}") long windowSeconds) {
        this.maxPerCustomer = Math.max(1, maxPerCustomer);
        this.maxPerIp = Math.max(1, maxPerIp);
        this.windowMillis = Duration.ofSeconds(Math.max(1, windowSeconds)).toMillis();
    }

    public void check(Long customerId, String clientIp) {
        long now = System.currentTimeMillis();
        boolean customerOk = hit("c:" + customerId, maxPerCustomer, now);
        boolean ipOk = hit("i:" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp), maxPerIp, now);
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis);
        }
        if (!customerOk || !ipOk) {
            throw new TooManyRequestsException("Bạn đã thử quá nhiều lần, vui lòng thử lại sau 10 phút");
        }
    }

    /** Tăng bộ đếm (kể cả khi đã vượt) và trả true nếu còn trong hạn mức. */
    private boolean hit(String key, int max, long now) {
        Window w = windows.compute(key, (ignored, current) ->
                current == null || now - current.startedAt >= windowMillis
                        ? new Window(now, 1) : new Window(current.startedAt, current.attempts + 1));
        return w.attempts <= max;
    }

    private record Window(long startedAt, int attempts) {
    }
}
