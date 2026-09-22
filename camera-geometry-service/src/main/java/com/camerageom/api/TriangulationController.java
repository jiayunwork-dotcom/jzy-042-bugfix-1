package com.camerageom.api;

import com.camerageom.api.dto.TriangulationJobRequest;
import com.camerageom.api.dto.TriangulationJobResponse;
import com.camerageom.jobs.TriangulationJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triangulation endpoint: two cameras (intrinsics + extrinsics each) plus the
 * matched pixel pairs between the two views. Uses the midpoint method and
 * reports per-match and job-level reprojection errors.
 */
@RestController
@RequestMapping("/api/v1/triangulations")
public class TriangulationController {

    private final TriangulationJobService triangulationJobs;

    public TriangulationController(TriangulationJobService triangulationJobs) {
        this.triangulationJobs = triangulationJobs;
    }

    @PostMapping("/jobs")
    public ResponseEntity<TriangulationJobResponse> submitJob(@RequestBody TriangulationJobRequest request) {
        return ResponseEntity.ok(triangulationJobs.process(request));
    }
}
