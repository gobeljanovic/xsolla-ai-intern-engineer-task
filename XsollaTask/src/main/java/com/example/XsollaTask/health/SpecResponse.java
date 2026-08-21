package com.example.XsollaTask.health;

import java.util.List;

public record SpecResponse(
        String specVersion,
        List<String> providers,
        SpecLimitsResponse limits
) {
}