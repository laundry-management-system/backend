package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderChannel {
    ONLINE("Online"),
    OFFLINE("Tại quầy");

    private final String description;
}
