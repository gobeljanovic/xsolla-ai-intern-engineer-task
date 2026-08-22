package com.example.XsollaTask.review.job;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JobStatus {
    QUEUED("queued"),
    RUNNING("running"),
    DONE("done"),
    FAILED("failed");

    private final String jsonValue;

    JobStatus(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }
}