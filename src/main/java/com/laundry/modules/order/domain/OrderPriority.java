package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderPriority {
    NORMAL("Bình thường", 0),
    HIGH("Ưu tiên", 1),
    URGENT("Gấp / Hỏa tốc", 2);

    private final String description;
    private final int level;
}

