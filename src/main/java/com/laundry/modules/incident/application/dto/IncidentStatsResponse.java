package com.laundry.modules.incident.application.dto;

import java.math.BigDecimal;

public record IncidentStatsResponse(long pending, long approved, long rejected, long all,
                                    BigDecimal paidCash, BigDecimal paidTransfer) {
}
