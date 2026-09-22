package com.camerageom.api.dto;

import java.util.List;

/**
 * Extrinsics as received over the wire: row-major 3x3 rotation, 3-vector translation.
 */
public record PoseDto(List<List<Double>> rotation, List<Double> translation) {
}
