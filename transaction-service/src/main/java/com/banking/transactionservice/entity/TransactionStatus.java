package com.banking.transactionservice.entity;

public enum TransactionStatus {

    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    FLAGGED,
    FAILED,
    COMPLETED
}
