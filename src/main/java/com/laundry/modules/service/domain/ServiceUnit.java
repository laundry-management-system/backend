package com.laundry.modules.service.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceUnit {
    KG("kg"),
    PIECE("món"),
    SET("bộ"),
    PRODUCT("sản phẩm");

    private final String displayName;
}
