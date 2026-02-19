package com.example.paymentsservice.exception;

/**
 * Thrown when a payment cannot be completed due to insufficient wallet balance.
 * Mapped to HTTP 422 (UNPROCESSABLE_ENTITY) by {@link GlobalExceptionHandler}.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
