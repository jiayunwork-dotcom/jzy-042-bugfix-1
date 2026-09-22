package com.camerageom.api;

import com.camerageom.api.dto.ProjectionJobRequest;
import com.camerageom.api.dto.ProjectionJobResponse;
import com.camerageom.api.dto.SingleProjectionRequest;
import com.camerageom.api.dto.SingleProjectionResponse;
import com.camerageom.jobs.ProjectionJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Projection endpoints. Batch jobs and single-point projection share the same
 * projection pipeline, so a point projected through either entry yields the
 * identical pixel.
 */
@RestController
@RequestMapping("/api/v1/projections")
public class ProjectionController {

    private final ProjectionJobService projectionJobs;

    public ProjectionController(ProjectionJobService projectionJobs) {
        this.projectionJobs = projectionJobs;
    }

    /** Batch projection job: one intrinsics/distortion set plus a batch of 3D points. */
    @PostMapping("/jobs")
    public ResponseEntity<ProjectionJobResponse> submitJob(@RequestBody ProjectionJobRequest request) {
        return ResponseEntity.ok(projectionJobs.process(request));
    }

    /** Single-point projection, for spot checks against batch job results. */
    @PostMapping("/point")
    public ResponseEntity<SingleProjectionResponse> projectSingle(@RequestBody SingleProjectionRequest request) {
        return ResponseEntity.ok(projectionJobs.processSingle(request));
    }
}
