package com.camerageom.api.dto;

public record StatusResponse(
        String status,
        long uptimeMillis,
        long projectionJobsProcessed,
        long triangulationJobsProcessed,
        long singleProjectionsProcessed,
        String serverTime) {
}
