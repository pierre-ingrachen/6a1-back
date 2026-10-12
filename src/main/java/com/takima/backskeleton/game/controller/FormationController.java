package com.takima.backskeleton.game.controller;

import com.takima.backskeleton.game.dto.FormationDto;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.service.GameViewMapper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("formations")
@RestController
public class FormationController {
    private final GameViewMapper gameViewMapper;

    public FormationController(GameViewMapper gameViewMapper) {
        this.gameViewMapper = gameViewMapper;
    }

    @GetMapping
    public List<FormationDto> findAllFormations() {
        return Arrays.stream(Formation.values()).map(gameViewMapper::toDto).toList();
    }
}
