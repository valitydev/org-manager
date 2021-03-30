package com.rbkmoney.orgmanager.converter;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import com.rbkmoney.orgmanager.config.properties.InviteTokenProperties;
import com.rbkmoney.orgmanager.entity.InvitationEntity;
import com.rbkmoney.orgmanager.util.JsonMapper;
import com.rbkmoney.swag.organizations.model.Invitation;
import com.rbkmoney.swag.organizations.model.InvitationAccepted;
import com.rbkmoney.swag.organizations.model.InvitationAcceptedAllOfMember;
import com.rbkmoney.swag.organizations.model.InvitationExpired;
import com.rbkmoney.swag.organizations.model.InvitationPending;
import com.rbkmoney.swag.organizations.model.InvitationRequest;
import com.rbkmoney.swag.organizations.model.InvitationRevoked;
import com.rbkmoney.swag.organizations.model.InvitationStatusName;
import com.rbkmoney.swag.organizations.model.Invitee;
import com.rbkmoney.swag.organizations.model.InviteeContact;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvitationConverter {

    private final JsonMapper jsonMapper;
    private final MemberRoleConverter memberRoleConverter;
    private final InviteTokenProperties inviteTokenProperties;

    public InvitationEntity toEntity(InvitationRequest invitation, String orgId) {
        Optional<Invitee> invitee = Optional.ofNullable(invitation.getInvitee());

        return InvitationEntity.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(orgId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(inviteTokenProperties.getLifeTimeInDays()))
                .inviteeContactType(invitee
                        .map(Invitee::getContact)
                        .map(InviteeContact::getType)
                        .map(InviteeContact.TypeEnum::getValue)
                        .orElse(null))
                .inviteeContactEmail(invitee
                        .map(Invitee::getContact)
                        .map(InviteeContact::getEmail)
                        .orElse(""))
                .inviteeRoles(invitee
                        .map(Invitee::getRoles)
                        .orElse(emptySet())
                        .stream()
                        .map(role -> memberRoleConverter.toEntity(role, orgId))
                        .collect(toSet()))
                .metadata(jsonMapper.toJson(invitation.getMetadata()))
                .status(InvitationStatusName.PENDING.getValue())
                .acceptToken(UUID.randomUUID().toString()) // TODO [a.romanov]: token
                .build();
    }

    public Invitation toDomain(InvitationEntity entity) {
        InvitationStatusName invitationStatusName = InvitationStatusName.fromValue(entity.getStatus());
        Invitation invitation = null;
        switch (invitationStatusName) {
            case PENDING:
                invitation = new InvitationPending();
                break;
            case ACCEPTED:
                invitation = new InvitationAccepted()
                      .acceptedAt(OffsetDateTime.from(entity.getAcceptedAt()))
                      .member(new InvitationAcceptedAllOfMember().id(entity.getAcceptedMemberId()));
                break;
            case EXPIRED:
                invitation = new InvitationExpired();
                break;
            case REVOKED:
                invitation = new InvitationRevoked()
                      .revokedAt(OffsetDateTime.from(entity.getRevokedAt()))
                      .reason(entity.getRevocationReason());
                break;
            default:
                invitation = new Invitation();
        }

        invitation.id(entity.getId())
              .createdAt(OffsetDateTime.of(entity.getCreatedAt(), ZoneOffset.UTC))
              .expiresAt(OffsetDateTime.of(entity.getExpiresAt(), ZoneOffset.UTC))
              .acceptToken(entity.getAcceptToken())
              .metadata(entity.getMetadata() != null ? jsonMapper.toMap(entity.getMetadata()) : null)
              .invitee(new Invitee()
                    .contact(new InviteeContact()
                          .type(InviteeContact.TypeEnum.fromValue(entity.getInviteeContactType()))
                          .email(entity.getInviteeContactEmail()))
                    .roles(entity.getInviteeRoles()
                          .stream()
                          .map(memberRoleConverter::toDomain)
                          .collect(toSet())));

        return invitation;
    }

}