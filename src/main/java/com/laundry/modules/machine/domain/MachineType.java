package com.laundry.modules.machine.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MachineType {
    WASHER("MG", "Máy giặt"),
    DRYER("MS", "Máy sấy");

    /** Tiền tố mã máy: MG01, MS01... */
    private final String codePrefix;
    private final String description;
}
