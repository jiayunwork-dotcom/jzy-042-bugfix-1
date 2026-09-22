package com.camerageom.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.camerageom.api.TestJson.obj;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Many jobs hitting the service at the same time must stay fully isolated:
 * each response carries exactly its own job's pixels and errors, and no job
 * ever sees another job's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConcurrencyIsolationWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    void concurrentProjectionJobsDoNotLeakIntoEachOther() throws Exception {
        int jobCount = 16;
        ExecutorService pool = Executors.newFixedThreadPool(jobCount);
        CountDownLatch ready = new CountDownLatch(jobCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<JsonNode>> futures = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            final int index = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                double fx = 600.0 + 25.0 * index;
                double fy = fx + 10.0;
                Map<String, Object> job = obj(
                        "intrinsics", obj("fx", fx, "fy", fy, "cx", 640.0, "cy", 360.0),
                        "image", obj("width", 1280, "height", 720),
                        "points", List.of(
                                obj("x", 0.4, "y", -0.3, "z", 2.5),
                                obj("x", -0.2, "y", 0.35, "z", 3.0)));
                String body = mvc.perform(post("/api/v1/projections/jobs")
                                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                return om.readTree(body);
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        Set<String> jobIds = new HashSet<>();
        for (int i = 0; i < jobCount; i++) {
            JsonNode response = futures.get(i).get(30, TimeUnit.SECONDS);
            double fx = 600.0 + 25.0 * i;
            double fy = fx + 10.0;
            // Each response must match ITS OWN intrinsics, not a neighbor's.
            assertEquals(640.0 + fx * 0.4 / 2.5, response.get("points").get(0).get("u").asDouble(), 1e-9);
            assertEquals(360.0 + fy * -0.3 / 2.5, response.get("points").get(0).get("v").asDouble(), 1e-9);
            assertEquals(640.0 + fx * -0.2 / 3.0, response.get("points").get(1).get("u").asDouble(), 1e-9);
            assertEquals(0, response.get("outOfBoundsCount").asInt());
            jobIds.add(response.get("jobId").asText());
        }
        assertEquals(jobCount, jobIds.size(), "every job must get its own id");
        pool.shutdownNow();
    }

    @Test
    void concurrentTriangulationJobsDoNotLeakIntoEachOther() throws Exception {
        int jobCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(jobCount);
        CountDownLatch ready = new CountDownLatch(jobCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<JsonNode>> futures = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            final double cubeCenterZ = 4.0 + 0.25 * i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                Map<String, Object> job = triangulationJobForCube(cubeCenterZ);
                String body = mvc.perform(post("/api/v1/triangulations/jobs")
                                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                return om.readTree(body);
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        for (int i = 0; i < jobCount; i++) {
            double cubeCenterZ = 4.0 + 0.25 * i;
            JsonNode response = futures.get(i).get(30, TimeUnit.SECONDS);
            assertEquals(8, response.get("matchCount").asInt());
            assertTrue(response.get("maxReprojectionErrorPixels").asDouble() < 1e-6);
            // Every recovered corner must belong to THIS job's cube depth.
            for (JsonNode match : response.get("matches")) {
                double z = match.get("point").get("z").asDouble();
                assertTrue(Math.abs(Math.abs(z - cubeCenterZ) - 0.5) < 1e-6,
                        "recovered z=" + z + " does not belong to cube at " + cubeCenterZ);
            }
        }
        pool.shutdownNow();
    }

    /** Matches for a unit cube centered at (0, 0, centerZ), seen by two pinhole cameras. */
    private Map<String, Object> triangulationJobForCube(double centerZ) {
        Map<String, Object> intrinsics = obj("fx", 1000.0, "fy", 1000.0, "cx", 960.0, "cy", 540.0);
        Map<String, Object> pose1 = obj(
                "rotation", List.of(List.of(1, 0, 0), List.of(0, 1, 0), List.of(0, 0, 1)),
                "translation", List.of(0, 0, 0));
        Map<String, Object> pose2 = obj(
                "rotation", List.of(List.of(1, 0, 0), List.of(0, 1, 0), List.of(0, 0, 1)),
                "translation", List.of(-1, 0, 0));
        List<Map<String, Object>> matches = new ArrayList<>();
        for (double dz : new double[]{-0.5, 0.5}) {
            for (double dy : new double[]{-0.5, 0.5}) {
                for (double dx : new double[]{-0.5, 0.5}) {
                    double z = centerZ + dz;
                    // view 1: X_cam = (dx, dy, z); view 2: X_cam = (dx - 1, dy, z)
                    matches.add(obj(
                            "u1", 960.0 + 1000.0 * dx / z,
                            "v1", 540.0 + 1000.0 * dy / z,
                            "u2", 960.0 + 1000.0 * (dx - 1.0) / z,
                            "v2", 540.0 + 1000.0 * dy / z));
                }
            }
        }
        return obj(
                "camera1", obj("intrinsics", intrinsics, "pose", pose1),
                "camera2", obj("intrinsics", intrinsics, "pose", pose2),
                "matches", matches);
    }
}
