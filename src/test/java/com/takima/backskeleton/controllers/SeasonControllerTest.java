package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.SeasonDto;
import com.takima.backskeleton.services.SeasonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeasonController.class)
class SeasonControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeasonService seasonService;

    @Test
    void findAllSeasonsReturnsOkWithSeasons() throws Exception {
        when(seasonService.findAllSeasons()).thenReturn(List.of(
                new SeasonDto((short) 2025, "2025/2026"),
                new SeasonDto((short) 2024, "2024/2025")
        ));

        mockMvc.perform(get("/seasons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startYear").value(2025))
                .andExpect(jsonPath("$[0].label").value("2025/2026"));
    }
}
