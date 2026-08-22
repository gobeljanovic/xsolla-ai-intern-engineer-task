package com.example.XsollaTask.common.api;

public final class InvalidJsonException
        extends RuntimeException {

    public InvalidJsonException() {
        super("Request body contains invalid JSON");
    }
}