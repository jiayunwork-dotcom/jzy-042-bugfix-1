package com.camerageom.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The built-in presets, the cube calibration example, and the status endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresetAndStatusWebTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void presetsEndpointEchoesRegisteredPresets() throws Exception {
        mvc.perform(get("/api/v1/presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("hd-1000"))
                .andExpect(jsonPath("$[0].intrinsics.fx").value(1000.0))
                .andExpect(jsonPath("$[1].name").value("vga-500"))
                .andExpect(jsonPath("$[2].name").value("hd-distorted-demo"));
    }

    @Test
    void cubeExampleProjectsEveryCornerInsideTheImage() throws Exception {
        mvc.perform(post("/api/v1/presets/cube-example/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointCount").value(8))
                .andExpect(jsonPath("$.outOfBoundsCount").value(0))
                .andExpect(jsonPath("$.points[0].inBounds").value(true))
                .andExpect(jsonPath("$.points[7].inBounds").value(true));
    }

    @Test
    void statusEndpointReportsUpAndJobCounters() throws Exception {
        mvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.projectionJobsProcessed").isNumber())
                .andExpect(jsonPath("$.triangulationJobsProcessed").isNumber());
    }
}
