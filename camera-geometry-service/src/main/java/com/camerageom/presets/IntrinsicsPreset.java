package com.camerageom.presets;

import com.camerageom.model.Distortion;
import com.camerageom.model.ImageSize;
import com.camerageom.model.Intrinsics;

/**
 * A named, pre-registered intrinsics preset echoed by the read-only presets endpoint.
 */
public record IntrinsicsPreset(
        String name,
        Intrinsics intrinsics,
        Distortion distortion,
        ImageSize imageSize,
        String description) {
}
