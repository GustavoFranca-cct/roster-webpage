package com.simpleroster.routegenerator.service;

import com.simpleroster.routegenerator.entity.ConfigurationSetting;
import com.simpleroster.routegenerator.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing application configuration settings stored in the database.
 * Allows retrieving single settings, groups of settings, and updating settings.
 */
@Service
@RequiredArgsConstructor
public class ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationService.class);
    private final ConfigurationRepository configurationRepository;

    /**
     * Retrieves all configuration settings stored in the database.
     *
     * @return A Map where keys are setting names and values are setting values.
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        log.debug("Fetching all configuration settings.");
        return configurationRepository.findAll().stream()
                .collect(Collectors.toMap(ConfigurationSetting::getSettingKey, ConfigurationSetting::getSettingValue));
    }

    /**
     * Retrieves all configuration settings whose keys start with the given prefix.
     *
     * @param prefix The prefix to filter setting keys (e.g., "ga.", "penalty.").
     * @return A Map containing only the settings matching the prefix.
     */
    @Transactional(readOnly = true)
    public Map<String, String> getSettingsByPrefix(String prefix) {
        log.debug("Fetching configuration settings with prefix: {}", prefix);
        return configurationRepository.findBySettingKeyStartingWith(prefix).stream()
                .collect(Collectors.toMap(ConfigurationSetting::getSettingKey, ConfigurationSetting::getSettingValue));
    }

    /**
     * Retrieves a single configuration setting by its key.
     * If the setting is not found, returns the provided default value.
     *
     * @param key          The unique key of the setting.
     * @param defaultValue The value to return if the setting is not found.
     * @return The setting's value or the default value.
     */
    @Transactional(readOnly = true)
    public String getSettingOrDefault(String key, String defaultValue) {
        log.trace("Fetching setting with key: {}, default: {}", key, defaultValue);
        // Use Optional handling for cleaner code
        return configurationRepository.findById(key)
                .map(ConfigurationSetting::getSettingValue)
                .orElseGet(() -> {
                    log.trace("Setting key '{}' not found, returning default value.", key);
                    return defaultValue;
                });
    }

    /**
     * Retrieves multiple configuration settings based on a list of keys.
     * Settings not found for a given key will be omitted from the result map.
     *
     * @param keys A List of setting keys to retrieve.
     * @return A Map containing the found settings (key-value pairs).
     */
    @Transactional(readOnly = true)
    public Map<String, String> getSettings(List<String> keys) {
        log.debug("Fetching settings for keys: {}", keys);
        Map<String, String> settings = new HashMap<>();
        configurationRepository.findAllById(keys).forEach(setting ->
                settings.put(setting.getSettingKey(), setting.getSettingValue()));
        log.debug("Found {} settings out of {} requested keys.", settings.size(), keys.size());
        return settings;
    }

    /**
     * Updates multiple configuration settings.
     * If a setting key exists, its value is updated.
     * If a setting key does not exist, a new setting is created.
     *
     * @param settingsToUpdate A Map where keys are setting names and values are the new desired values.
     */
    @Transactional
    public void updateSettings(Map<String, String> settingsToUpdate) {
        log.info("Updating {} configuration settings.", settingsToUpdate.size());
        if (settingsToUpdate == null || settingsToUpdate.isEmpty()) {
            log.warn("Update settings called with no settings to update.");
            return;
        }

        // Fetch existing settings efficiently to check for updates vs inserts
        List<ConfigurationSetting> existingSettings = configurationRepository.findAllById(settingsToUpdate.keySet());
        Map<String, ConfigurationSetting> existingMap = existingSettings.stream()
                .collect(Collectors.toMap(ConfigurationSetting::getSettingKey, s -> s));

        // Prepare list of entities to save (either updated or new)
        List<ConfigurationSetting> toSave = settingsToUpdate.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    ConfigurationSetting setting = existingMap.get(key);
                    if (setting == null) {
                        // Create new setting if it doesn't exist
                        log.debug("Creating new setting: {} = {}", key, value);
                        setting = new ConfigurationSetting(key, value);
                    } else {
                        // Update existing setting value if it has changed
                        if (!setting.getSettingValue().equals(value)) {
                             log.debug("Updating existing setting: {} = {}", key, value);
                             setting.setSettingValue(value);
                        } else {
                             log.trace("Setting {} value unchanged, skipping update.", key);
                             // Optionally return null or filter later to avoid saving unchanged entities
                             // return null; // Example: avoids unnecessary save
                        }
                    }
                    return setting;
                })
                // .filter(Objects::nonNull) // Filter out nulls if using the optimization above
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            log.debug("Saving {} updated/new settings.", toSave.size());
            configurationRepository.saveAll(toSave);
            log.info("Configuration settings updated successfully.");
        } else {
            log.info("No actual changes required for the provided settings update.");
        }
    }
} 