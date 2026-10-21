package com.laundry.modules.service.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceUnit {
    KG("kg"),
    PIECE("piece"),
    SET("set"),
    PRODUCT("product");

    private final String displayName;
}
