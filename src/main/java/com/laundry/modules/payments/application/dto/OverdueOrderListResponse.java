package com.laundry.modules.payments.application.dto;

import java.util.List;

public record OverdueOrderListResponse(int overdueDays, List<OverdueOrderResponse> orders) {
}
