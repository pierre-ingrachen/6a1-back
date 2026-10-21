package com.takima.backskeleton.config;

import com.takima.backskeleton.controllers.SeasonController;
import com.takima.backskeleton.services.SeasonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeasonController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://front.example,http://admin.example")
class CorsConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeasonService seasonService;

    @Test
    void allowedOriginReceivesCorsHeaders() throws Exception {
        mockMvc.perform(get("/seasons").header(HttpHeaders.ORIGIN, "http://front.example"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://front.example"));
    }

    @Test
    void everyConfiguredOriginIsAllowed() throws Exception {
        mockMvc.perform(get("/seasons").header(HttpHeaders.ORIGIN, "http://admin.example"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://admin.example"));
    }

    @Test
    void unknownOriginIsForbidden() throws Exception {
        mockMvc.perform(get("/seasons").header(HttpHeaders.ORIGIN, "http://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, nullValue()));
    }

    @Test
    void preflightFromAllowedOriginIsAccepted() throws Exception {
        mockMvc.perform(options("/seasons")
                        .header(HttpHeaders.ORIGIN, "http://front.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://front.example"));
    }
}
