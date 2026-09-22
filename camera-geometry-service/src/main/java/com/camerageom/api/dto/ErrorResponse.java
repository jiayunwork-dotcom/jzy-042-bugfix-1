package com.camerageom.api.dto;

import java.util.Map;

/**
 * Structured, typed error body returned for every rejected job.
 */
public record ErrorResponse(String type, String message, Map<String, Object> details) {
}
