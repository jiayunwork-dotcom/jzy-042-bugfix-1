package com.camerageom.api;

import com.camerageom.api.dto.StatusResponse;
import com.camerageom.jobs.JobMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Liveness/status endpoint.
 */
@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final JobMetrics metrics;

    public StatusController(JobMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping
    public ResponseEntity<StatusResponse> status() {
        return ResponseEntity.ok(new StatusResponse(
                "UP",
                metrics.uptimeMillis(),
                metrics.projectionJobsProcessed(),
                metrics.triangulationJobsProcessed(),
                metrics.singleProjectionsProcessed(),
                Instant.now().toString()));
    }
}
