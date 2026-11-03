package com.yobbu.order.domain.model;

public enum DeliveryStatus {
    PENDING_ASSIGNMENT,
    ASSIGNED,
    HEADING_TO_MERCHANT,
    AT_MERCHANT,
    HEADING_TO_CUSTOMER,
    DELIVERED,
    FAILED
}
