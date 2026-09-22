package com.camerageom.api.dto;

/**
 * Intrinsics as received over the wire. Boxed types so that a missing field
 * arrives as null and is rejected with a typed validation error.
 */
public record IntrinsicsDto(Double fx, Double fy, Double cx, Double cy) {
}
