package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.CreateInvitationRequest;
import dev.vality.orgmanagement.InvalidInvitationState;
import dev.vality.orgmanagement.InvalidRequest;
import dev.vality.orgmanagement.Invitation;
import dev.vality.orgmanagement.InvitationNotFound;
import dev.vality.orgmanagement.InvitationStatus;
import dev.vality.orgmanagement.ListInvitationsRequest;
import dev.vality.orgmanagement.ListInvitationsResult;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.RevokeInvitationRequest;
import dev.vality.orgmanagement.RoleAssignment;
import dev.vality.orgmanager.config.properties.InviteTokenProperties;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.StoredInvitationStatus;
import dev.vality.orgmanager.entity.StoredInviteeContactType;
import dev.vality.orgmanager.repository.InvitationRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.vality.orgmanager.service.AdminCommonService.collectionOrEmpty;

/**
 * Приглашения в организации в административном контракте.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInvitationService {

    static final int DEFAULT_INVITATION_LIMIT = 20;
    static final int MAX_INVITATION_LIMIT = 1000;

    private final InvitationRepository invitationRepository;
    private final InviteTokenProperties inviteTokenProperties;
    private final MailMessageSender mailMessageSender;
    private final AdminManagementConverter converter;
    private final AdminCommonService commonService;

    @Transactional
    public Invitation create(String organizationId, CreateInvitationRequest request)
            throws OrganizationNotFound, InvalidRequest {
        log.info("Create invitation: organizationId={}, email={}", organizationId, request.getEmail());
        commonService.findOrganization(organizationId);
        String email = commonService.requireText(request.getEmail(), "Email");
        String metadata = commonService.toStoredMetadata(request.getMetadata());
        Set<MemberRoleEntity> roles = new LinkedHashSet<>();
        for (RoleAssignment role : collectionOrEmpty(request.getRoles())) {
            commonService.validateRoleAssignment(organizationId, role.getRoleId(), role.getScope());
            roles.add(commonService.toMemberRoleEntity(organizationId, role.getRoleId(), role.getScope()));
        }
        LocalDateTime now = LocalDateTime.now();
        InvitationEntity invitation = InvitationEntity.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .createdAt(now)
                .expiresAt(now.plusDays(inviteTokenProperties.getLifeTimeInDays()))
                .acceptToken(UUID.randomUUID().toString())
                .inviteeContactType(StoredInviteeContactType.EMAIL.getValue())
                .inviteeContactEmail(email)
                .inviteeRoles(roles)
                .metadata(metadata)
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
    public ListInvitationsResult list(String organizationId, ListInvitationsRequest request)
            throws OrganizationNotFound {
        log.info("List invitations: organizationId={}, request={}", organizationId, request);
        commonService.findOrganization(organizationId);
        ListInvitationsRequest safeRequest = request == null ? new ListInvitationsRequest() : request;
        int limit = invitationLimit(safeRequest);

        InvitationEntity cursor = null;
        if (safeRequest.isSetContinuationToken()) {
            cursor = invitationRepository
                    .findByIdAndOrganizationId(safeRequest.getContinuationToken(), organizationId)
                    .orElse(null);
            if (cursor == null) {
                return new ListInvitationsResult(List.of());
            }
        }

        Pageable pageable = PageRequest.of(0, limit + 1,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Specification<InvitationEntity> specification = specification(
                organizationId,
                safeRequest.isSetStatus() ? safeRequest.getStatus() : null,
                LocalDateTime.now(),
                cursor);
        List<InvitationEntity> page = new ArrayList<>(
                invitationRepository.findAll(specification, pageable).getContent());

        String continuationToken = null;
        if (page.size() > limit) {
            page = new ArrayList<>(page.subList(0, limit));
            continuationToken = page.get(page.size() - 1).getId();
        }
        ListInvitationsResult result = new ListInvitationsResult(
                page.stream().map(converter::toInvitation).toList());
        if (continuationToken != null) {
            result.setContinuationToken(continuationToken);
        }
        return result;
    }

    /**
     * Отозвать можно только приглашение в статусе pending.
     */
    @Transactional
    public void revoke(String organizationId, String invitationId, RevokeInvitationRequest request)
            throws OrganizationNotFound, InvitationNotFound, InvalidInvitationState {
        log.info("Revoke invitation: organizationId={}, invitationId={}", organizationId, invitationId);
        commonService.findOrganization(organizationId);
        InvitationEntity invitation = findInvitation(organizationId, invitationId);
        InvitationStatus status = converter.effectiveInvitationStatus(invitation);
        if (status != InvitationStatus.pending) {
            throw new InvalidInvitationState(
                    "Expected invitation status pending, but was " + status.name());
        }
        invitation.setStatus(StoredInvitationStatus.REVOKED.getValue());
        invitation.setRevocationReason(request.getReason());
        invitation.setRevokedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    /**
     * Страница упорядочена по (created_at, id) убыванию, поэтому продолжение читается keyset-ом
     * от приглашения, на которое указывает continuation token.
     */
    private Specification<InvitationEntity> specification(
            String organizationId,
            InvitationStatus status,
            LocalDateTime now,
            InvitationEntity cursor) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("organizationId"), organizationId));
            if (status != null) {
                predicates.add(statusPredicate(root, builder, status, now));
            }
            if (cursor != null) {
                Path<LocalDateTime> createdAt = root.get("createdAt");
                predicates.add(builder.or(
                        builder.lessThan(createdAt, cursor.getCreatedAt()),
                        builder.and(
                                builder.equal(createdAt, cursor.getCreatedAt()),
                                builder.lessThan(root.get("id"), cursor.getId()))));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }


    private Predicate statusPredicate(
            Root<InvitationEntity> root,
            CriteriaBuilder builder,
            InvitationStatus status,
            LocalDateTime now) {
        Path<String> stored = root.get("status");
        Predicate storedPending = builder.equal(stored, StoredInvitationStatus.PENDING.getValue());
        return switch (status) {
            case pending -> builder.and(storedPending, builder.greaterThan(root.get("expiresAt"), now));
            case expired -> builder.or(
                    builder.equal(stored, StoredInvitationStatus.EXPIRED.getValue()),
                    builder.and(storedPending, builder.lessThanOrEqualTo(root.get("expiresAt"), now)));
            case accepted -> builder.equal(stored, StoredInvitationStatus.ACCEPTED.getValue());
            case revoked -> builder.equal(stored, StoredInvitationStatus.REVOKED.getValue());
        };
    }

    private int invitationLimit(ListInvitationsRequest request) {
        if (!request.isSetLimit() || request.getLimit() <= 0) {
            return DEFAULT_INVITATION_LIMIT;
        }
        return Math.min(request.getLimit(), MAX_INVITATION_LIMIT);
    }

    private InvitationEntity findInvitation(String organizationId, String invitationId) throws InvitationNotFound {
        return invitationRepository.findByIdAndOrganizationId(invitationId, organizationId)
                .orElseThrow(InvitationNotFound::new);
    }
}
