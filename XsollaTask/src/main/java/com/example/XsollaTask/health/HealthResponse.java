package com.example.XsollaTask.health;

public record HealthResponse(
        String status,
        String version,
        long uptimeSeconds
) {
}