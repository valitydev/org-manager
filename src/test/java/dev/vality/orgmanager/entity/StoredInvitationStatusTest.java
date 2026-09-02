package dev.vality.orgmanager.entity;

import dev.vality.swag.organizations.model.InvitationStatusName;
import dev.vality.swag.organizations.model.InviteeContact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Значения статусов и типа контакта продублированы из swagger-модели, чтобы thrift-слой от неё
 * не зависел. Тест ловит расхождение при обновлении swagger-контракта: без него разошедшиеся
 * литералы молча испортили бы данные в колонках invitation.status / invitation.invitee_contact_type.
 */
class StoredInvitationStatusTest {

    @Test
    void shouldMatchSwaggerInvitationStatusValues() {
        assertEquals(InvitationStatusName.PENDING.getValue(), StoredInvitationStatus.PENDING.getValue());
        assertEquals(InvitationStatusName.ACCEPTED.getValue(), StoredInvitationStatus.ACCEPTED.getValue());
        assertEquals(InvitationStatusName.EXPIRED.getValue(), StoredInvitationStatus.EXPIRED.getValue());
        assertEquals(InvitationStatusName.REVOKED.getValue(), StoredInvitationStatus.REVOKED.getValue());
    }

    @Test
    void shouldMatchSwaggerInviteeContactTypeValue() {
        assertEquals(InviteeContact.TypeEnum.E_MAIL.getValue(), StoredInviteeContactType.EMAIL.getValue());
    }

    @Test
    void shouldParseStatusIgnoringCase() {
        assertEquals(StoredInvitationStatus.PENDING, StoredInvitationStatus.fromValue("pending"));
        assertEquals(StoredInvitationStatus.REVOKED, StoredInvitationStatus.fromValue("Revoked"));
    }
}
