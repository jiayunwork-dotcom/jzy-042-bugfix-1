package com.camerageom.jobs;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-wide counters only. Job inputs, intermediates and results are never
 * stored here, so concurrent jobs cannot observe or corrupt each other.
 */
@Component
public class JobMetrics {

    private final Instant startedAt = Instant.now();
    private final AtomicLong projectionJobs = new AtomicLong();
    private final AtomicLong triangulationJobs = new AtomicLong();
    private final AtomicLong singleProjections = new AtomicLong();

    public void projectionJobCompleted() {
        projectionJobs.incrementAndGet();
    }

    public void triangulationJobCompleted() {
        triangulationJobs.incrementAndGet();
    }

    public void singleProjectionCompleted() {
        singleProjections.incrementAndGet();
    }

    public long projectionJobsProcessed() {
        return projectionJobs.get();
    }

    public long triangulationJobsProcessed() {
        return triangulationJobs.get();
    }

    public long singleProjectionsProcessed() {
        return singleProjections.get();
    }

    public long uptimeMillis() {
        return Instant.now().toEpochMilli() - startedAt.toEpochMilli();
    }
}
