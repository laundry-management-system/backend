package com.laundry.modules.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemUnit {
    KG("kg"),
    PIECE("món"),
    SET("bộ"),
    PRODUCT("sản phẩm");

    private final String displayName;
}

