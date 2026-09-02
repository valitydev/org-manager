package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.CreateInvitationRequest;
import dev.vality.orgmanagement.InvalidOrganizationState;
import dev.vality.orgmanagement.Invitation;
import dev.vality.orgmanagement.InvitationNotFound;
import dev.vality.orgmanagement.InvitationStatus;
import dev.vality.orgmanagement.ListInvitationsRequest;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.RevokeInvitationRequest;
import dev.vality.orgmanager.config.properties.InviteTokenProperties;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.StoredInvitationStatus;
import dev.vality.orgmanager.entity.StoredInviteeContactType;
import dev.vality.orgmanager.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Приглашения в организации в административном контракте.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInvitationService {

    private final InvitationRepository invitationRepository;
    private final InviteTokenProperties inviteTokenProperties;
    private final MailMessageSender mailMessageSender;
    private final AdminManagementConverter converter;
    private final AdminCommonService commonService;

    @Transactional
    public Invitation create(String organizationId, CreateInvitationRequest request) throws OrganizationNotFound {
        log.info("Create invitation: organizationId={}, email={}", organizationId, request.getEmail());
        commonService.findOrganization(organizationId);
        Set<MemberRoleEntity> roles = request.getRoles().stream()
                .map(role -> commonService.toMemberRoleEntity(organizationId, role.getRoleId(), role.getScope()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LocalDateTime now = LocalDateTime.now();
        InvitationEntity invitation = InvitationEntity.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .createdAt(now)
                .expiresAt(now.plusDays(inviteTokenProperties.getLifeTimeInDays()))
                .acceptToken(UUID.randomUUID().toString())
                .inviteeContactType(StoredInviteeContactType.EMAIL.getValue())
                .inviteeContactEmail(request.getEmail())
                .inviteeRoles(roles)
                .metadata(commonService.toStoredMetadata(request.getMetadata()))
                .status(StoredInvitationStatus.PENDING.getValue())
                .build();
        invitation = invitationRepository.save(invitation);
        mailMessageSender.send(invitation.getAcceptToken(), invitation.getInviteeContactEmail());
        return converter.toInvitation(invitation);
    }

    @Transactional(readOnly = true)
    public Invitation get(String organizationId, String invitationId)
            throws OrganizationNotFound, InvitationNotFound {
        log.info("Get invitation: organizationId={}, invitationId={}", organizationId, invitationId);
        commonService.findOrganization(organizationId);
        return converter.toInvitation(findInvitation(organizationId, invitationId));
    }

    @Transactional(readOnly = true)
    public List<Invitation> list(String organizationId, ListInvitationsRequest request) throws OrganizationNotFound {
        log.info("List invitations: organizationId={}, request={}", organizationId, request);
        commonService.findOrganization(organizationId);
        InvitationStatus status = request == null ? null : request.getStatus();
        return invitationRepository.findByOrganizationId(organizationId).stream()
                .filter(invitation -> status == null || converter.effectiveInvitationStatus(invitation) == status)
                .sorted(Comparator.comparing(InvitationEntity::getCreatedAt).reversed())
                .map(converter::toInvitation)
                .toList();
    }

    @Transactional
    public void revoke(String organizationId, String invitationId, RevokeInvitationRequest request)
            throws OrganizationNotFound, InvitationNotFound, InvalidOrganizationState {
        log.info("Revoke invitation: organizationId={}, invitationId={}", organizationId, invitationId);
        commonService.findOrganization(organizationId);
        InvitationEntity invitation = findInvitation(organizationId, invitationId);
        InvitationStatus status = converter.effectiveInvitationStatus(invitation);
        if (status != InvitationStatus.pending) {
            throw new InvalidOrganizationState("Only a pending invitation can be revoked");
        }
        invitation.setStatus(StoredInvitationStatus.REVOKED.getValue());
        invitation.setRevocationReason(request.getReason());
        invitation.setRevokedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    private InvitationEntity findInvitation(String organizationId, String invitationId) throws InvitationNotFound {
        return invitationRepository.findByIdAndOrganizationId(invitationId, organizationId)
                .orElseThrow(InvitationNotFound::new);
    }
}
