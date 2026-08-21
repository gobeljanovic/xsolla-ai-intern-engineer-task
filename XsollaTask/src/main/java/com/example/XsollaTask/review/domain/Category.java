package com.example.XsollaTask.review.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    SECURITY("security"),
    CORRECTNESS("correctness"),
    PERFORMANCE("performance"),
    STYLE("style");

    private final String jsonValue;

    Category(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }
}