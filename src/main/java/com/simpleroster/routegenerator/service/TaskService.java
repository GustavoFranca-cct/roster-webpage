package com.simpleroster.routegenerator.service;

import com.simpleroster.routegenerator.dto.TaskDTO;
import com.simpleroster.routegenerator.entity.Task;
import com.simpleroster.routegenerator.repository.TaskRepository;
import com.simpleroster.routegenerator.repository.SkillRepository;
import com.simpleroster.routegenerator.entity.Skill;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Service layer for managing Task entities.
 * Handles CRUD operations for tasks, including managing required skills.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;
    private final SkillRepository skillRepository;

    /**
     * Retrieves a list of all tasks.
     *
     * @return List of TaskDTOs.
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasks() {
        log.info("Fetching all tasks.");
        List<Task> tasks = taskRepository.findAll();
        log.info("Found {} tasks.", tasks.size());
        return tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // --- Mappers ---

    /**
     * Maps a Task entity to a TaskDTO.
     *
     * @param task The Task entity.
     * @return The corresponding TaskDTO.
     */
    private TaskDTO mapToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setName(task.getName());
        dto.setDescription(task.getDescription());
        dto.setMinimumCoverage(task.getMinimumCoverage());
        dto.setOptimalCoverage(task.getOptimalCoverage());
        dto.setPenaltyWeight(task.getPenaltyWeight());
        if (task.getRequiredSkills() != null) {
            dto.setRequiredSkills(task.getRequiredSkills().stream()
                                    .map(Skill::getName)
                                    .collect(Collectors.toSet()));
        } else {
            dto.setRequiredSkills(new HashSet<>());
        }
        return dto;
    }

    /**
     * Maps a TaskDTO to a Task entity.
     * Sets default values for coverage and weight if not provided in the DTO.
     * Does not map skills here; skill mapping is handled during create/update.
     *
     * @param dto The TaskDTO.
     * @return The corresponding Task entity (not yet persisted, skills not set).
     */
    private Task mapToEntity(TaskDTO dto) {
        Task task = new Task();
        task.setName(dto.getName());
        task.setDescription(dto.getDescription());
        task.setMinimumCoverage(dto.getMinimumCoverage() != null ? dto.getMinimumCoverage() : 1);
        task.setOptimalCoverage(dto.getOptimalCoverage() != null ? dto.getOptimalCoverage() : 1);
        task.setPenaltyWeight(dto.getPenaltyWeight() != null ? dto.getPenaltyWeight() : 10);
        return task;
    }

    // --- CRUD Methods ---

    /**
     * Creates a new task.
     * Validates that the task name is unique.
     * Associates required skills based on skill names provided in the DTO.
     *
     * @param taskDTO DTO containing the new task's details and required skill names.
     * @return The created TaskDTO.
     * @throws IllegalArgumentException if a task with the same name already exists.
     */
    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        log.info("Attempting to create task: {}", taskDTO.getName());
        taskRepository.findByName(taskDTO.getName()).ifPresent(t -> {
            log.warn("Task creation failed: Name '{}' already exists.", taskDTO.getName());
            throw new IllegalArgumentException("Task with name '" + taskDTO.getName() + "' already exists.");
        });
        Task task = mapToEntity(taskDTO);

        // Map and assign required skills
        if (taskDTO.getRequiredSkills() != null && !taskDTO.getRequiredSkills().isEmpty()) {
            log.debug("Finding required skills for new task '{}': {}", task.getName(), taskDTO.getRequiredSkills());
            Set<Skill> skills = skillRepository.findByNameIn(taskDTO.getRequiredSkills());
            // TODO: Consider validating if all requested skill names were found?
            log.debug("Assigning {} skills to new task '{}'.", skills.size(), task.getName());
            task.setRequiredSkills(skills);
        } else {
            log.debug("No required skills specified for new task '{}'.", task.getName());
        }

        Task savedTask = taskRepository.save(task);
        log.info("Task '{}' created successfully with ID: {}", savedTask.getName(), savedTask.getId());
        return mapToDTO(savedTask);
    }

    /**
     * Updates an existing task.
     * Allows updating description, coverage, weight, and required skills.
     * Validates name uniqueness if the name is changed.
     *
     * @param id      The ID of the task to update.
     * @param taskDTO DTO containing the updated task details.
     * @return The updated TaskDTO.
     * @throws EntityNotFoundException  if the task is not found.
     * @throws IllegalArgumentException if the new name conflicts with another existing task.
     */
    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        log.info("Attempting to update task with ID: {}", id);
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed: Task not found with ID: {}", id);
                    return new EntityNotFoundException("Task not found with id: " + id);
                });

        // Check for name conflict only if the name is actually changing
        if (!existingTask.getName().equals(taskDTO.getName())) {
            log.debug("Task name change detected for ID {}. Checking for conflicts with new name: {}", id, taskDTO.getName());
            taskRepository.findByName(taskDTO.getName()).ifPresent(t -> {
                if (!t.getId().equals(id)) {
                    log.warn("Update failed for ID {}: New name '{}' conflicts with existing task ID {}.", id, taskDTO.getName(), t.getId());
                    throw new IllegalArgumentException("Task with name '" + taskDTO.getName() + "' already exists.");
                }
            });
            existingTask.setName(taskDTO.getName());
        }

        log.trace("Updating details for task ID: {}", id);
        existingTask.setDescription(taskDTO.getDescription());
        // Use current value if DTO field is null to avoid accidental resets
        existingTask.setMinimumCoverage(taskDTO.getMinimumCoverage() != null ? taskDTO.getMinimumCoverage() : existingTask.getMinimumCoverage());
        existingTask.setOptimalCoverage(taskDTO.getOptimalCoverage() != null ? taskDTO.getOptimalCoverage() : existingTask.getOptimalCoverage());
        existingTask.setPenaltyWeight(taskDTO.getPenaltyWeight() != null ? taskDTO.getPenaltyWeight() : existingTask.getPenaltyWeight());

        // Update required skills - Replace the entire set based on the DTO
        if (taskDTO.getRequiredSkills() != null) {
             log.debug("Updating required skills for task ID {}: {}", id, taskDTO.getRequiredSkills());
            Set<Skill> newSkills = skillRepository.findByNameIn(taskDTO.getRequiredSkills());
            // TODO: Validate if all requested skill names were found?
             log.debug("Assigning {} skills to task ID {}.", newSkills.size(), id);
            existingTask.setRequiredSkills(newSkills);
        } else {
             log.debug("Removing all required skills for task ID {}.", id);
            existingTask.setRequiredSkills(new HashSet<>()); // Set to empty if null in DTO
        }

        Task updatedTask = taskRepository.save(existingTask);
        log.info("Task {} updated successfully.", id);
        return mapToDTO(updatedTask);
    }

    /**
     * Deletes a task by its ID.
     * Note: This does not currently check for dependencies (e.g., if the task is used in shifts).
     * Depending on cascade rules or business logic, deletion might fail or leave orphaned data.
     *
     * @param id The ID of the task to delete.
     * @throws EntityNotFoundException if the task is not found.
     */
    @Transactional
    public void deleteTask(Long id) {
        log.info("Attempting to delete task with ID: {}", id);
        if (!taskRepository.existsById(id)) {
            log.warn("Deletion failed: Task not found with ID: {}", id);
            throw new EntityNotFoundException("Task not found with id: " + id);
        }
        // TODO: Add check for dependencies before deletion (e.g., check ShiftRepository)
        try {
            taskRepository.deleteById(id);
            log.info("Task {} deleted successfully.", id);
        } catch (Exception e) {
            // Catch potential constraint violation errors if DB prevents deletion due to dependencies
            log.error("Error deleting task {}: {}. Possible dependencies exist.", id, e.getMessage());
            // Rethrow a more specific or application-level exception if needed
            throw new RuntimeException("Could not delete task with ID " + id + ". It might be in use.", e);
        }
    }
} 