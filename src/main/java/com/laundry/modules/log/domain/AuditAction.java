package com.laundry.modules.log.domain;

import lombok.Getter;

@Getter
public enum AuditAction {
    ACCOUNT_MANAGE,
    ACCOUNT_LOCKED,
    CUSTOMER_PHONE_CHANGE,
    DUE_DATE_CHANGE(true),
    INCIDENT_APPROVE,
    MACHINE_STATUS,
    ORDER_PRICE_EDIT,
    PAYMENT_CONFIRM,
    PAYMENT_VOID(true),
    PRICE_CHANGE,
    SET_UP_PASSWORD,
    STATUS_ROLLBACK(true),
    SETTING_UPDATED;

    private final boolean reasonRequired;

    AuditAction() { this(false); }
    AuditAction(boolean reasonRequired) { this.reasonRequired = reasonRequired; }

}