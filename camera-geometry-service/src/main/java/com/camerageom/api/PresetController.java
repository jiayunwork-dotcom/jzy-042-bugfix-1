package com.camerageom.api;

import com.camerageom.api.dto.DistortionDto;
import com.camerageom.api.dto.ImageSizeDto;
import com.camerageom.api.dto.IntrinsicsDto;
import com.camerageom.api.dto.Point3DDto;
import com.camerageom.api.dto.ProjectionJobRequest;
import com.camerageom.api.dto.ProjectionJobResponse;
import com.camerageom.jobs.ProjectionJobService;
import com.camerageom.model.Point3D;
import com.camerageom.presets.CubeCalibrationExample;
import com.camerageom.presets.IntrinsicsPreset;
import com.camerageom.presets.IntrinsicsPresetRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only echo of the registered intrinsics presets, plus the built-in cube
 * calibration example (and a convenience runner that projects it).
 */
@RestController
@RequestMapping("/api/v1/presets")
public class PresetController {

    private final IntrinsicsPresetRegistry registry;
    private final CubeCalibrationExample cubeExample;
    private final ProjectionJobService projectionJobs;

    public PresetController(IntrinsicsPresetRegistry registry,
                            CubeCalibrationExample cubeExample,
                            ProjectionJobService projectionJobs) {
        this.registry = registry;
        this.cubeExample = cubeExample;
        this.projectionJobs = projectionJobs;
    }

    /** Read-only echo of every registered intrinsics preset. */
    @GetMapping
    public ResponseEntity<List<IntrinsicsPreset>> listPresets() {
        return ResponseEntity.ok(registry.all());
    }

    /** The built-in cube calibration example (preset + known cube corners). */
    @GetMapping("/cube-example")
    public ResponseEntity<CubeExampleView> cubeExample() {
        IntrinsicsPreset preset = registry.byName(cubeExample.presetName()).orElseThrow();
        return ResponseEntity.ok(new CubeExampleView(
                cubeExample.presetName(),
                cubeExample.description(),
                preset.intrinsics(),
                preset.distortion(),
                preset.imageSize(),
                cubeExample.corners()));
    }

    /**
     * Runs the cube example through the standard projection job pipeline.
     * Every corner must land inside the image (outOfBoundsCount == 0).
     */
    @PostMapping("/cube-example/run")
    public ResponseEntity<ProjectionJobResponse> runCubeExample() {
        IntrinsicsPreset preset = registry.byName(cubeExample.presetName()).orElseThrow();
        List<Point3DDto> points = cubeExample.corners().stream()
                .map(p -> new Point3DDto(p.x(), p.y(), p.z()))
                .toList();
        ProjectionJobRequest request = new ProjectionJobRequest(
                new IntrinsicsDto(
                        preset.intrinsics().fx(), preset.intrinsics().fy(),
                        preset.intrinsics().cx(), preset.intrinsics().cy()),
                new DistortionDto(
                        preset.distortion().k1(), preset.distortion().k2(),
                        preset.distortion().p1(), preset.distortion().p2()),
                new ImageSizeDto(preset.imageSize().width(), preset.imageSize().height()),
                points);
        return ResponseEntity.ok(projectionJobs.process(request));
    }

    /** Wire view of the cube example; domain records serialize directly. */
    public record CubeExampleView(
            String presetName,
            String description,
            com.camerageom.model.Intrinsics intrinsics,
            com.camerageom.model.Distortion distortion,
            com.camerageom.model.ImageSize imageSize,
            List<Point3D> cubeCorners) {
    }
}
