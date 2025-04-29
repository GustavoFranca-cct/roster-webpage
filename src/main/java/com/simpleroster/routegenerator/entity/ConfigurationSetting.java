package com.simpleroster.routegenerator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single key-value configuration setting stored in the database.
 * Allows dynamic configuration of application parameters (e.g., GA settings, penalties).
 */
@Entity
@Table(name = "configuration_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // Lombok constructor for all fields
public class ConfigurationSetting {

    /**
     * The unique key identifying the configuration setting.
     * Examples: "ga.population.size", "penalty.under.staffing".
     * Serves as the primary key.
     */
    @Id
    @Column(name = "setting_key", nullable = false, unique = true)
    private String settingKey;

    /**
     * The value of the configuration setting, stored as a String.
     * Type conversion (e.g., to Integer, Double, LocalTime) happens in the service layer.
     */
    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

//    Constructor replaced by Lombok's @AllArgsConstructor
//    public ConfigurationSetting(String settingKey, String settingValue) {
//        this.settingKey = settingKey;
//        this.settingValue = settingValue;
//    }
} 