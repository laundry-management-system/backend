package com.laundry.modules.incident.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedoStage {
    WASH("Giặt"),
    DRY("Sấy"),
    BOTH("Cả hai"),
    NONE("Không");

    private final String description;
}
