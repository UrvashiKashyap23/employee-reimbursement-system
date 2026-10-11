package com.Urvashi.expense_reimbursement_system.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    public BusinessRuleException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessRuleException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}