package com.example.XsollaTask.review.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProviderType {
    MOCK("mock"),
    LLM("llm");

    private final String jsonValue;

    ProviderType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static ProviderType fromJson(String value) {
        for (ProviderType type : values()) {
            if (type.jsonValue.equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unknown review provider: " + value
        );
    }
}