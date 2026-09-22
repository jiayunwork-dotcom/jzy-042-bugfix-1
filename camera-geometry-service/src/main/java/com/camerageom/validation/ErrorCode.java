package com.camerageom.validation;

/**
 * Typed error codes for job rejection. Every invalid input is rejected with one
 * of these BEFORE any computation starts.
 */
public enum ErrorCode {
    MISSING_INTRINSICS_FIELD,
    NON_POSITIVE_FOCAL_LENGTH,
    INVALID_IMAGE_SIZE,
    MISSING_DISTORTION_FIELD,
    EMPTY_POINT_LIST,
    INVALID_POINT_COORDINATE,
    POINT_BEHIND_CAMERA,
    MISSING_CAMERA,
    MISSING_CAMERA_POSE,
    INVALID_POSE,
    EMPTY_MATCH_LIST,
    INVALID_MATCH,
    TRIANGULATION_DEGENERATE,
    MALFORMED_REQUEST,
    INTERNAL_ERROR
}
