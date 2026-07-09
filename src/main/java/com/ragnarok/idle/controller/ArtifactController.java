package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.ArtifactDto;
import com.ragnarok.idle.service.ArtifactService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only API справочника артефактов. */
@RestController
@RequestMapping("/api/artifacts")
public class ArtifactController {

    private final ArtifactService artifactService;

    public ArtifactController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping
    public List<ArtifactDto> list() {
        return artifactService.findAll();
    }
}
