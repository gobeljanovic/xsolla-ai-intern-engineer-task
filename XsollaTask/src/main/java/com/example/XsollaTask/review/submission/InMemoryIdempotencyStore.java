package com.example.XsollaTask.review.submission;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Component
public final class InMemoryIdempotencyStore {

    private final ConcurrentMap<String, IdempotencyRecord> records =
            new ConcurrentHashMap<>();

    public String resolve(
            String idempotencyKey,
            byte[] requestBody,
            Supplier<String> jobCreator
    ) {
        Objects.requireNonNull(requestBody);
        Objects.requireNonNull(jobCreator);

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {
            return jobCreator.get();
        }

        String fingerprint = fingerprint(requestBody);

        IdempotencyRecord record = records.compute(
                idempotencyKey,
                (key, existing) -> {
                    if (existing == null) {
                        return new IdempotencyRecord(
                                fingerprint,
                                jobCreator.get()
                        );
                    }

                    if (!existing.bodyFingerprint()
                            .equals(fingerprint)) {
                        throw new IdempotencyConflictException();
                    }

                    return existing;
                }
        );

        return record.jobId();
    }

    private String fingerprint(byte[] body) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(body)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}