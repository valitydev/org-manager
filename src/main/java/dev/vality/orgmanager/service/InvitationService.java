package dev.vality.orgmanager.service;

import dev.vality.orgmanager.converter.InvitationConverter;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.exception.InviteAlreadyAcceptedException;
import dev.vality.orgmanager.exception.InviteExpiredException;
import dev.vality.orgmanager.exception.InviteRevokedException;
import dev.vality.orgmanager.exception.ResourceNotFoundException;
import dev.vality.orgmanager.repository.InvitationRepository;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.swag.organizations.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationConverter invitationConverter;
    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final MailMessageSender mailMessageSender;

    // TODO [a.romanov]: idempotency
    @Transactional
    public Invitation create(
            String orgId,
            InvitationRequest invitation,
            String idempotencyKey) {
        InvitationEntity entity = invitationConverter.toEntity(invitation, orgId);
        InvitationEntity savedEntity = invitationRepository.save(entity);
        Invitation savedInvitation = invitationConverter.toDomain(savedEntity);
        mailMessageSender.send(savedEntity.getAcceptToken(), savedEntity.getInviteeContactEmail());
        return savedInvitation;
    }

    public ResponseEntity<Invitation> get(String invitationId) {
        Optional<InvitationEntity> entity = invitationRepository.findById(invitationId);

        if (entity.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        }

        Invitation invitation = invitationConverter.toDomain(entity.orElseThrow());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(invitation);
    }

    public ResponseEntity<InvitationListResult> list(String orgId, InvitationStatusName status) {
        boolean isOrganizationExist = organizationRepository.existsById(orgId);

        if (!isOrganizationExist) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        }

        List<InvitationEntity> entities = status != null
                ? invitationRepository.findByOrganizationIdAndStatus(orgId, status.getValue())
                : invitationRepository.findByOrganizationId(orgId);

        List<Invitation> invitations = entities.stream()
                .filter(invite -> !isExpiredPendingInvitation(invite))
                .map(invitationConverter::toDomain)
                .collect(toList());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new InvitationListResult()
                        .result(invitations));
    }

    @Transactional
    public void revoke(String orgId, String invitationId, InlineObject1 inlineObject) {
        InvitationEntity invitation = invitationRepository.findByIdAndOrganizationId(invitationId, orgId)
                .orElseThrow(ResourceNotFoundException::new);
        invitation.setStatus(inlineObject.getStatus().getValue());
        invitation.setRevocationReason(inlineObject.getReason());
        invitation.setRevokedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

    }

    @Transactional
    public void checkAndModifyExpiredStatus() {
        Stream<InvitationEntity> invitationEntity = invitationRepository.findAllPendingStatus();
        invitationEntity.forEach(invitation -> {
            if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
                invitation.setStatus(InvitationStatusName.EXPIRED.getValue());
            }
        });
    }

    public InvitationEntity findByToken(String token) {
        InvitationEntity invitationEntity = invitationRepository.findByAcceptToken(token)
                .orElseThrow(ResourceNotFoundException::new);
        validateInvitation(invitationEntity);
        return invitationEntity;
    }

    private void validateInvitation(InvitationEntity invitationEntity) {
        if (invitationEntity.isExpired()) {
            throw new InviteExpiredException(invitationEntity.getExpiresAt().toString());
        }
        if (invitationEntity.getStatus().equalsIgnoreCase(InvitationStatusName.REVOKED.getValue())) {
            throw new InviteRevokedException(invitationEntity.getRevocationReason());
        }
        if (invitationEntity.getStatus().equalsIgnoreCase(InvitationStatusName.ACCEPTED.getValue())) {
            throw new InviteAlreadyAcceptedException(invitationEntity.getAcceptedAt().toString());
        }
    }

    private boolean isExpiredPendingInvitation(InvitationEntity invitationEntity) {
        return invitationEntity.getStatus() != null
                && invitationEntity.getStatus().equalsIgnoreCase(InvitationStatusName.PENDING.getValue())
                && invitationEntity.isExpired();
    }

}
