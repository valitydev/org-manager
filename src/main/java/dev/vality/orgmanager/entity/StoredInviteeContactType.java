package dev.vality.orgmanager.entity;

/**
 * Значения колонки {@code invitation.invitee_contact_type}.
 */
public enum StoredInviteeContactType {

    EMAIL("EMail");

    private final String value;

    StoredInviteeContactType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
