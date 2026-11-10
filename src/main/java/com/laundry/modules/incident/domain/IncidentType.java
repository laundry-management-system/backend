package com.laundry.modules.incident.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncidentType {
    LOST("Mất"),
    TORN("Rách"),
    FADED("Phai màu"),
    SHRUNK("Co rút"),
    OTHER("Khác");

    private final String description;
}
