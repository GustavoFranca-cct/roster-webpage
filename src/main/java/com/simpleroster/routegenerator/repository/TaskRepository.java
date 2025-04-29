package com.simpleroster.routegenerator.repository;

import com.simpleroster.routegenerator.entity.Skill;
import com.simpleroster.routegenerator.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Method to find a task by its name
    Optional<Task> findByName(String name);
    //method to create a task by its name



} 