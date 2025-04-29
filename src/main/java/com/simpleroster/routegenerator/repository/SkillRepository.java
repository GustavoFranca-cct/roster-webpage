package com.simpleroster.routegenerator.repository;

import com.simpleroster.routegenerator.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByName(String name);
    Set<Skill> findByNameIn(Set<String> names); // Find skills by a set of names
} 