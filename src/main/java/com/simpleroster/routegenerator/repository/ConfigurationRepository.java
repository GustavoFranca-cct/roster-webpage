package com.simpleroster.routegenerator.repository;

import com.simpleroster.routegenerator.entity.ConfigurationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfigurationRepository extends JpaRepository<ConfigurationSetting, String> {

    // Optional: Find multiple settings by key prefix if needed later
    List<ConfigurationSetting> findBySettingKeyStartingWith(String prefix);
} 