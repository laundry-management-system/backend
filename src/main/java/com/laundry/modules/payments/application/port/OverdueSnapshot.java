package com.laundry.modules.payments.application.port;

public record OverdueSnapshot(OrderSnapshot order, boolean customerVip) {
}
