package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.SeasonDto;
import com.takima.backskeleton.services.SeasonService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("seasons")
@RestController
public class SeasonController {
    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping
    public List<SeasonDto> findAllSeasons() {
        return seasonService.findAllSeasons();
    }
}
