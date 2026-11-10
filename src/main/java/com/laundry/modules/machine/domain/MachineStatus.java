package com.laundry.modules.machine.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MachineStatus {
    READY("Sẵn sàng"),
    MAINTENANCE("Bảo trì"),
    RETIRED("Ngừng sử dụng");

    private final String description;
}
