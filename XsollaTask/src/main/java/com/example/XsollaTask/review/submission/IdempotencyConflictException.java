package com.example.XsollaTask.review.submission;

public final class IdempotencyConflictException
        extends RuntimeException {

    public IdempotencyConflictException() {
        super( "Idempotency key was already used " + "with a different request body" );
    }
}