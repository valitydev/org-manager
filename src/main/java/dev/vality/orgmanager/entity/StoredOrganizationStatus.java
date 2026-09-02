package dev.vality.orgmanager.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Значения колонки {@code organization.status}.
 */
@Getter
@RequiredArgsConstructor
public enum StoredOrganizationStatus {

    ACTIVE("active"),
    DEACTIVATED("deactivated");

    private final String value;

    public boolean matches(String status) {
        return value.equalsIgnoreCase(status);
    }

    public static StoredOrganizationStatus fromValue(String status) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.matches(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown organization status: " + status));
    }
}
