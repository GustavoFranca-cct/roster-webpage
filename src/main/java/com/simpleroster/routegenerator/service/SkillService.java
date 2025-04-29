package com.simpleroster.routegenerator.service;

import com.simpleroster.routegenerator.dto.SkillDTO; // Create this DTO
import com.simpleroster.routegenerator.entity.Skill;
import com.simpleroster.routegenerator.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing Skill entities.
 * Handles listing and creating skills.
 */
@Service
@RequiredArgsConstructor
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);
    private final SkillRepository skillRepository;

    /**
     * Retrieves a list of all skills.
     *
     * @return List of SkillDTOs.
     */
    @Transactional(readOnly = true)
    public List<SkillDTO> getAllSkills() {
        log.info("Fetching all skills.");
        List<Skill> skills = skillRepository.findAll();
        log.info("Found {} skills.", skills.size());
        return skills.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new skill.
     * Validates that the skill name is unique.
     *
     * @param skillDTO DTO containing the name for the new skill.
     * @return The created SkillDTO.
     * @throws IllegalArgumentException if a skill with the same name already exists.
     */
    @Transactional
    public SkillDTO createSkill(SkillDTO skillDTO) {
        log.info("Attempting to create skill: {}", skillDTO.getName());
        // Validate name is not blank (Controller might also do this)
        if (skillDTO.getName() == null || skillDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name cannot be empty.");
        }
        // Check for existing skill with the same name (case-insensitive check might be better)
        skillRepository.findByName(skillDTO.getName()).ifPresent(s -> {
            log.warn("Skill creation failed: Name '{}' already exists.", skillDTO.getName());
            throw new IllegalArgumentException("Skill with name '" + skillDTO.getName() + "' already exists.");
        });

        Skill newSkill = new Skill(skillDTO.getName());
        Skill savedSkill = skillRepository.save(newSkill);
        log.info("Skill '{}' created successfully with ID: {}", savedSkill.getName(), savedSkill.getId());
        return mapToDTO(savedSkill);
    }

    // TODO: Add deleteSkill(Long id) method - consider dependencies (Employees, Tasks)
    // TODO: Add updateSkill(Long id, SkillDTO skillDTO) method if needed

    /**
     * Maps a Skill entity to a SkillDTO.
     *
     * @param skill The Skill entity.
     * @return The corresponding SkillDTO.
     */
    private SkillDTO mapToDTO(Skill skill) {
        return new SkillDTO(skill.getId(), skill.getName());
    }
} 