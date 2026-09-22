package com.camerageom.presets;

import com.camerageom.model.Distortion;
import com.camerageom.model.ImageSize;
import com.camerageom.model.Intrinsics;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of built-in intrinsics presets. Read-only at runtime: presets are
 * fixed at startup and never mutated by jobs.
 */
@Component
public class IntrinsicsPresetRegistry {

    private final Map<String, IntrinsicsPreset> presets;

    public IntrinsicsPresetRegistry() {
        Map<String, IntrinsicsPreset> registered = new LinkedHashMap<>();
        register(registered, new IntrinsicsPreset(
                "hd-1000",
                new Intrinsics(1000.0, 1000.0, 960.0, 540.0),
                Distortion.ZERO,
                new ImageSize(1920, 1080),
                "Full-HD pinhole, f = 1000 px, zero distortion"));
        register(registered, new IntrinsicsPreset(
                "vga-500",
                new Intrinsics(500.0, 500.0, 320.0, 240.0),
                Distortion.ZERO,
                new ImageSize(640, 480),
                "VGA pinhole, f = 500 px, zero distortion"));
        register(registered, new IntrinsicsPreset(
                "hd-distorted-demo",
                new Intrinsics(1100.0, 1080.0, 960.0, 540.0),
                new Distortion(-0.12, 0.015, 0.001, -0.0005),
                new ImageSize(1920, 1080),
                "Full-HD with mild barrel distortion, for distortion-path checks"));
        // unmodifiableMap keeps the LinkedHashMap registration order, so the
        // read-only echo is stable and deterministic.
        this.presets = Collections.unmodifiableMap(registered);
    }

    private static void register(Map<String, IntrinsicsPreset> target, IntrinsicsPreset preset) {
        target.put(preset.name(), preset);
    }

    public List<IntrinsicsPreset> all() {
        return List.copyOf(presets.values());
    }

    public Optional<IntrinsicsPreset> byName(String name) {
        return Optional.ofNullable(presets.get(name));
    }
}
