package com.laundry.modules.booking.presentation;

import com.laundry.modules.booking.application.OnlineBookingService;
import com.laundry.modules.booking.application.dto.CreateBookingIntakeRequest;
import com.laundry.modules.booking.application.dto.OnlineBookingResponse;
import com.laundry.modules.customer.application.CustomerAuthRateLimiter;
import com.laundry.modules.customer.application.CustomerSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer Portal", description = "Khách gửi yêu cầu đặt đơn online")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer-portal/bookings")
public class CustomerPortalBookingController {

    private static final int MAX_PER_ACCOUNT = 10;
    private static final int MAX_PER_SOURCE = 10;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final CustomerSessionService customerSessionService;
    private final OnlineBookingService onlineBookingService;
    private final CustomerAuthRateLimiter rateLimiter;

    @Operation(summary = "Khách gửi 1-2 yêu cầu đặt đơn (thường/nhanh)", description = "Gửi lại cùng requestId trả về yêu cầu cũ")
    @PostMapping("/intake")
    @ResponseStatus(HttpStatus.CREATED)
    public OnlineBookingResponse createIntake(Authentication authentication,
                                              @Valid @RequestBody CreateBookingIntakeRequest request,
                                              HttpServletRequest http) {
        Long customerId = customerSessionService.requireCurrent(authentication).getId();
        rateLimiter.check("booking-account", "c" + customerId, MAX_PER_ACCOUNT, WINDOW);
        rateLimiter.check("booking-source", CustomerAuthRateLimiter.clientKey(http), MAX_PER_SOURCE, WINDOW);
        return onlineBookingService.createIntake(customerId, request);
    }
}
