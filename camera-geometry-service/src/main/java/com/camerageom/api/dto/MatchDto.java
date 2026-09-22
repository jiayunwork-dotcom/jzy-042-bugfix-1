package com.camerageom.api.dto;

/**
 * One matched pair of pixels: (u1, v1) in view 1, (u2, v2) in view 2.
 */
public record MatchDto(Double u1, Double v1, Double u2, Double v2) {
}
