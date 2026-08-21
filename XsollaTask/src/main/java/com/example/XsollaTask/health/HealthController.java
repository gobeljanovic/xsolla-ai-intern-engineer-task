package com.example.XsollaTask.health;

import com.example.XsollaTask.config.ServiceProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public final class HealthController {

    private final ServiceProperties serviceProperties;
    private final long startedAtNanos;

    public HealthController(ServiceProperties serviceProperties) {
        this.serviceProperties = serviceProperties;
        this.startedAtNanos = System.nanoTime();
    }

    @GetMapping("/health")
    public HealthResponse health() {
        long elapsedNanos = System.nanoTime() - startedAtNanos;
        long uptimeSeconds =
                TimeUnit.NANOSECONDS.toSeconds(elapsedNanos);

        return new HealthResponse(
                "ok",
                serviceProperties.version(),
                uptimeSeconds
        );
    }
}