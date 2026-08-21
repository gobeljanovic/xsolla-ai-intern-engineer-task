package com.example.XsollaTask.review.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Severity {
    CRITICAL("critical"),
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    private final String jsonValue;

    Severity(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }
}