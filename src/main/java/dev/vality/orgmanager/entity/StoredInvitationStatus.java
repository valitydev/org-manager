package dev.vality.orgmanager.entity;

import java.util.Arrays;

/**
 * Значения колонки {@code invitation.status}.
 */
public enum StoredInvitationStatus {

    PENDING("Pending"),
    ACCEPTED("Accepted"),
    EXPIRED("Expired"),
    REVOKED("Revoked");

    private final String value;

    StoredInvitationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean matches(String status) {
        return value.equalsIgnoreCase(status);
    }

    public static StoredInvitationStatus fromValue(String status) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.matches(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown invitation status: " + status));
    }
}
