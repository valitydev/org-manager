package dev.vality.orgmanager.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Значения колонки {@code invitation.invitee_contact_type}.
 */
@Getter
@RequiredArgsConstructor
public enum StoredInviteeContactType {

    EMAIL("EMail");

    private final String value;
}
