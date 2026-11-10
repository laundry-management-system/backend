package com.laundry.modules.log.domain;

import lombok.Getter;

@Getter
public enum AuditAction {
    ACCOUNT_MANAGE,
    ACCOUNT_LOCKED,
    CUSTOMER_PHONE_CHANGE,
    CUSTOMER_STATUS,
    CUSTOMER_VIP,
    DUE_DATE_CHANGE(true),
    INCIDENT_CREATE,
    INCIDENT_APPROVE,
    MACHINE_MANAGE,
    MACHINE_STATUS,
    ORDER_CANCEL(true),
    ORDER_PRICE_EDIT(true),
    PASSWORD_RESET,
    PAYMENT_CONFIRM,
    PAYMENT_VOID(true),
    PAYMENT_REFUND(true),
    PRICE_CHANGE,
    SERVICE_STATUS,
    SET_UP_PASSWORD,
    STATUS_ROLLBACK(true),
    SETTING_UPDATED,
    VOUCHER_MANAGE;

    private final boolean reasonRequired;

    AuditAction() { this(false); }
    AuditAction(boolean reasonRequired) { this.reasonRequired = reasonRequired; }

}
