package com.simpleroster.routegenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing an alert or notification
 * to be displayed on the dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO {
    /** The content of the alert message. */
    private String message;
    /** The severity level of the alert (e.g., "info", "warning", "error"). Used for styling. */
    private String level; // e.g., "info", "warning", "error"
} 