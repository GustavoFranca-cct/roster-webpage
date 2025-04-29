package com.simpleroster.routegenerator.controller;

import com.simpleroster.routegenerator.dto.SkillDTO;
import com.simpleroster.routegenerator.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST Controller for managing Skills.
 * Handles listing and creating skills.
 * Base path for all endpoints in this controller is /api/skills.
 */
@RestController
@RequestMapping("/api/skills") // Base path for skill-related endpoints
@RequiredArgsConstructor
@CrossOrigin // TODO: Configure CORS more restrictively for production environments!
public class SkillController {

    private final SkillService skillService;
    private static final Logger log = LoggerFactory.getLogger(SkillController.class);

    /**
     * Retrieves a list of all available skills.
     *
     * @return A list of SkillDTO objects.
     */
    @GetMapping
    public List<SkillDTO> getAllSkills() {
        log.info("GET /skills requested.");
        List<SkillDTO> skills = skillService.getAllSkills();
        log.info("Returning {} skills.", skills.size());
        return skills;
    }

    /**
     * Creates a new skill.
     *
     * @param skillDTO DTO containing the name of the skill to create.
     * @return ResponseEntity containing the created SkillDTO (201 Created), or 400 Bad Request if input is invalid
     *         (e.g., empty name, duplicate name), or 500 Internal Server Error for other issues.
     */
    @PostMapping
    public ResponseEntity<SkillDTO> createSkill(@RequestBody SkillDTO skillDTO) {
         log.info("POST /skills requested for skill: {}", skillDTO.getName());
         // Basic validation
        if (skillDTO.getName() == null || skillDTO.getName().trim().isEmpty()) {
             log.warn("Skill creation failed: Name is empty.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill name cannot be empty");
        }
        try {
            SkillDTO createdSkill = skillService.createSkill(skillDTO);
            log.info("Skill '{}' created successfully with ID: {}", createdSkill.getName(), createdSkill.getId());
            return new ResponseEntity<>(createdSkill, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) { // Catch duplicate name errors from service
            log.warn("Skill creation failed for '{}': {}", skillDTO.getName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error creating skill '{}':", skillDTO.getName(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating skill", e);
        }
    }

    // TODO: Add PUT /api/skills/{id} endpoint if updating skill names is required.
    // TODO: Add DELETE /api/skills/{id} endpoint if deleting skills is required (handle potential dependencies).
} 