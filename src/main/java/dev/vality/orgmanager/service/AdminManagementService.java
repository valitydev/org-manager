package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.AddMemberRequest;
import dev.vality.orgmanagement.AdminManagementSrv;
import dev.vality.orgmanagement.AssignMemberRoleRequest;
import dev.vality.orgmanagement.CreateInvitationRequest;
import dev.vality.orgmanagement.CreateOrganizationRequest;
import dev.vality.orgmanagement.InvalidOrganizationState;
import dev.vality.orgmanagement.Invitation;
import dev.vality.orgmanagement.InvitationNotFound;
import dev.vality.orgmanagement.InvitationStatus;
import dev.vality.orgmanagement.ListInvitationsRequest;
import dev.vality.orgmanagement.ListOrganizationsRequest;
import dev.vality.orgmanagement.ListOrganizationsResult;
import dev.vality.orgmanagement.Member;
import dev.vality.orgmanagement.MemberNotFound;
import dev.vality.orgmanagement.MemberRole;
import dev.vality.orgmanagement.MemberRoleNotFound;
import dev.vality.orgmanagement.Organization;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.OrganizationRole;
import dev.vality.orgmanagement.OrganizationStatus;
import dev.vality.orgmanagement.PartyAlreadyBound;
import dev.vality.orgmanagement.RevokeInvitationRequest;
import dev.vality.orgmanager.config.properties.InviteTokenProperties;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.orgmanager.repository.InvitationRepository;
import dev.vality.orgmanager.repository.MemberRepository;
import dev.vality.orgmanager.repository.MemberRoleRepository;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.repository.OrganizationRoleRepository;
import dev.vality.swag.organizations.model.InvitationStatusName;
import dev.vality.swag.organizations.model.InviteeContact;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminManagementService implements AdminManagementSrv.Iface {

    static final int DEFAULT_ORGANIZATION_LIMIT = 20;
    static final int MAX_ORGANIZATION_LIMIT = 1000;

    private static final String ACTIVE_STATUS = OrganizationStatus.active.name();
    private static final String DEACTIVATED_STATUS = OrganizationStatus.deactivated.name();

    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final InvitationRepository invitationRepository;
    private final AdminManagementConverter converter;
    private final InviteTokenProperties inviteTokenProperties;
    private final MailMessageSender mailMessageSender;

    @Override
    @Transactional
    public Organization createOrganization(CreateOrganizationRequest request) throws PartyAlreadyBound {
        if (organizationRepository.existsByParty(request.getPartyId())) {
            throw new PartyAlreadyBound();
        }
        OrganizationEntity entity = OrganizationEntity.builder()
                .id(UUID.randomUUID().toString())
                .party(request.getPartyId())
                .owner(request.getOwnerId())
                .name(request.getName())
                .metadata(request.getMetadata())
                .createdAt(LocalDateTime.now())
                .status(ACTIVE_STATUS)
                .members(new HashSet<>())
                .roles(new HashSet<>())
                .build();
        try {
            return converter.toOrganization(organizationRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new PartyAlreadyBound();
        }
    }

    @Override
    public Organization getOrganization(String organizationId) throws OrganizationNotFound {
        return converter.toOrganization(findOrganization(organizationId));
    }

    @Override
    public ListOrganizationsResult listOrganizations(ListOrganizationsRequest request) {
        ListOrganizationsRequest safeRequest = request == null ? new ListOrganizationsRequest() : request;
        int limit = organizationLimit(safeRequest);
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<OrganizationEntity> entities = findOrganizations(safeRequest, pageable);

        String continuationToken = null;
        if (entities.size() > limit) {
            entities = new ArrayList<>(entities.subList(0, limit));
            continuationToken = entities.get(entities.size() - 1).getId();
        }
        ListOrganizationsResult result = new ListOrganizationsResult(
                entities.stream().map(converter::toOrganization).toList());
        if (continuationToken != null) {
            result.setContinuationToken(continuationToken);
        }
        return result;
    }

    @Override
    @Transactional
    public Organization renameOrganization(String organizationId, String name) throws OrganizationNotFound {
        OrganizationEntity organization = findOrganization(organizationId);
        organization.setName(name);
        return converter.toOrganization(organizationRepository.save(organization));
    }

    @Override
    @Transactional
    public Organization deactivateOrganization(String organizationId)
            throws OrganizationNotFound, InvalidOrganizationState {
        return changeOrganizationStatus(organizationId, ACTIVE_STATUS, DEACTIVATED_STATUS);
    }

    @Override
    @Transactional
    public Organization activateOrganization(String organizationId)
            throws OrganizationNotFound, InvalidOrganizationState {
        return changeOrganizationStatus(organizationId, DEACTIVATED_STATUS, ACTIVE_STATUS);
    }

    @Override
    public Member getMember(String organizationId, String userId)
            throws OrganizationNotFound, MemberNotFound {
        OrganizationEntity organization = findOrganization(organizationId);
        return converter.toMember(findMember(organization, userId), organizationId);
    }

    @Override
    public List<Member> listMembers(String organizationId) throws OrganizationNotFound {
        OrganizationEntity organization = findOrganization(organizationId);
        return collectionOrEmpty(organization.getMembers()).stream()
                .sorted(Comparator.comparing(MemberEntity::getId))
                .map(member -> converter.toMember(member, organizationId))
                .toList();
    }

    @Override
    @Transactional
    public Member addMember(String organizationId, AddMemberRequest request) throws OrganizationNotFound {
        OrganizationEntity organization = findOrganization(organizationId);
        MemberEntity member = memberRepository.findById(request.getUserId())
                .orElseGet(() -> MemberEntity.builder()
                        .id(request.getUserId())
                        .roles(new HashSet<>())
                        .organizations(new HashSet<>())
                        .build());
        if (request.isSetEmail()) {
            member.setEmail(request.getEmail());
        }
        member = memberRepository.save(member);
        Set<MemberEntity> members = new HashSet<>(collectionOrEmpty(organization.getMembers()));
        members.add(member);
        organization.setMembers(members);
        organizationRepository.save(organization);
        return converter.toMember(member, organizationId);
    }

    @Override
    @Transactional
    public void removeMember(String organizationId, String userId)
            throws OrganizationNotFound, MemberNotFound {
        OrganizationEntity organization = findOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);

        Set<MemberRoleEntity> retainedRoles = new HashSet<>();
        List<MemberRoleEntity> removedRoles = new ArrayList<>();
        for (MemberRoleEntity role : collectionOrEmpty(member.getRoles())) {
            if (organizationId.equals(role.getOrganizationId()) && role.isActive()) {
                role.setActive(false);
                removedRoles.add(role);
            } else {
                retainedRoles.add(role);
            }
        }
        member.setRoles(retainedRoles);
        memberRoleRepository.saveAll(removedRoles);
        memberRepository.save(member);

        Set<MemberEntity> members = new HashSet<>(collectionOrEmpty(organization.getMembers()));
        members.remove(member);
        organization.setMembers(members);
        organizationRepository.save(organization);
    }

    @Override
    @Transactional
    public MemberRole assignMemberRole(
            String organizationId,
            String userId,
            AssignMemberRoleRequest request) throws OrganizationNotFound, MemberNotFound {
        OrganizationEntity organization = findOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);
        MemberRoleEntity role = toMemberRoleEntity(organizationId, request.getRoleId(), request.getScope());
        role = memberRoleRepository.save(role);
        Set<MemberRoleEntity> roles = new HashSet<>(collectionOrEmpty(member.getRoles()));
        roles.add(role);
        member.setRoles(roles);
        memberRepository.save(member);
        return converter.toMemberRole(role);
    }

    @Override
    @Transactional
    public void removeMemberRole(String organizationId, String userId, String memberRoleId)
            throws OrganizationNotFound, MemberNotFound, MemberRoleNotFound {
        OrganizationEntity organization = findOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);
        MemberRoleEntity role = collectionOrEmpty(member.getRoles()).stream()
                .filter(candidate -> candidate.getId().equals(memberRoleId))
                .filter(candidate -> organizationId.equals(candidate.getOrganizationId()))
                .filter(MemberRoleEntity::isActive)
                .findFirst()
                .orElseThrow(MemberRoleNotFound::new);
        role.setActive(false);
        Set<MemberRoleEntity> roles = new HashSet<>(collectionOrEmpty(member.getRoles()));
        roles.remove(role);
        member.setRoles(roles);
        memberRoleRepository.save(role);
        memberRepository.save(member);
    }

    @Override
    public OrganizationRole getOrganizationRole(String organizationId, String roleId)
            throws OrganizationNotFound {
        findOrganization(organizationId);
        OrganizationRoleEntity role = organizationRoleRepository.findByOrganizationIdAndRoleId(organizationId, roleId)
                .orElseThrow(OrganizationNotFound::new);
        return converter.toOrganizationRole(role);
    }

    @Override
    public List<OrganizationRole> listOrganizationRoles(String organizationId) throws OrganizationNotFound {
        OrganizationEntity organization = findOrganization(organizationId);
        return collectionOrEmpty(organization.getRoles()).stream()
                .sorted(Comparator.comparing(OrganizationRoleEntity::getRoleId))
                .map(converter::toOrganizationRole)
                .toList();
    }

    @Override
    @Transactional
    public Invitation createInvitation(String organizationId, CreateInvitationRequest request)
            throws OrganizationNotFound {
        findOrganization(organizationId);
        Set<MemberRoleEntity> roles = request.getRoles().stream()
                .map(role -> toMemberRoleEntity(organizationId, role.getRoleId(), role.getScope()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        LocalDateTime now = LocalDateTime.now();
        InvitationEntity invitation = InvitationEntity.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .createdAt(now)
                .expiresAt(now.plusDays(inviteTokenProperties.getLifeTimeInDays()))
                .acceptToken(UUID.randomUUID().toString())
                .inviteeContactType(InviteeContact.TypeEnum.E_MAIL.getValue())
                .inviteeContactEmail(request.getEmail())
                .inviteeRoles(roles)
                .metadata(request.getMetadata())
                .status(InvitationStatusName.PENDING.getValue())
                .build();
        invitation = invitationRepository.save(invitation);
        mailMessageSender.send(invitation.getAcceptToken(), invitation.getInviteeContactEmail());
        return converter.toInvitation(invitation);
    }

    @Override
    public Invitation getInvitation(String organizationId, String invitationId)
            throws OrganizationNotFound, InvitationNotFound {
        findOrganization(organizationId);
        InvitationEntity invitation = invitationRepository.findByIdAndOrganizationId(invitationId, organizationId)
                .orElseThrow(InvitationNotFound::new);
        return converter.toInvitation(invitation);
    }

    @Override
    public List<Invitation> listInvitations(String organizationId, ListInvitationsRequest request)
            throws OrganizationNotFound {
        findOrganization(organizationId);
        InvitationStatus status = request == null ? null : request.getStatus();
        return invitationRepository.findByOrganizationId(organizationId).stream()
                .filter(invitation -> status == null || converter.effectiveInvitationStatus(invitation) == status)
                .sorted(Comparator.comparing(InvitationEntity::getCreatedAt).reversed())
                .map(converter::toInvitation)
                .toList();
    }

    @Override
    @Transactional
    public void revokeInvitation(String organizationId, String invitationId, RevokeInvitationRequest request)
            throws OrganizationNotFound, InvitationNotFound, InvalidOrganizationState {
        findOrganization(organizationId);
        InvitationEntity invitation = invitationRepository.findByIdAndOrganizationId(invitationId, organizationId)
                .orElseThrow(InvitationNotFound::new);
        InvitationStatus status = converter.effectiveInvitationStatus(invitation);
        if (status != InvitationStatus.pending) {
            throw new InvalidOrganizationState("Only a pending invitation can be revoked");
        }
        invitation.setStatus(InvitationStatusName.REVOKED.getValue());
        invitation.setRevocationReason(request.getReason());
        invitation.setRevokedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    private List<OrganizationEntity> findOrganizations(ListOrganizationsRequest request, Pageable pageable) {
        String token = request.getContinuationToken();
        OrganizationStatus status = request.getStatus();
        if (status != null && token != null) {
            return organizationRepository.findByStatusAndIdLessThanOrderByIdDesc(status.name(), token, pageable);
        }
        if (status != null) {
            return organizationRepository.findByStatusOrderByIdDesc(status.name(), pageable);
        }
        if (token != null) {
            return organizationRepository.findByIdLessThanOrderByIdDesc(token, pageable);
        }
        return organizationRepository.findAllByOrderByIdDesc(pageable);
    }

    private int organizationLimit(ListOrganizationsRequest request) {
        if (!request.isSetLimit() || request.getLimit() <= 0) {
            return DEFAULT_ORGANIZATION_LIMIT;
        }
        return Math.min(request.getLimit(), MAX_ORGANIZATION_LIMIT);
    }

    private Organization changeOrganizationStatus(String organizationId, String expected, String target)
            throws OrganizationNotFound, InvalidOrganizationState {
        OrganizationEntity organization = findOrganization(organizationId);
        String current = organization.getStatus() == null ? ACTIVE_STATUS : organization.getStatus();
        if (!expected.equals(current)) {
            throw new InvalidOrganizationState(
                    "Expected organization status " + expected + ", but was " + current);
        }
        organization.setStatus(target);
        return converter.toOrganization(organizationRepository.save(organization));
    }

    private OrganizationEntity findOrganization(String organizationId) throws OrganizationNotFound {
        return organizationRepository.findById(organizationId)
                .orElseThrow(OrganizationNotFound::new);
    }

    private MemberEntity findMember(OrganizationEntity organization, String userId) throws MemberNotFound {
        return collectionOrEmpty(organization.getMembers()).stream()
                .filter(member -> member.getId().equals(userId))
                .findFirst()
                .orElseThrow(MemberNotFound::new);
    }

    private MemberRoleEntity toMemberRoleEntity(
            String organizationId,
            String roleId,
            dev.vality.orgmanagement.RoleScope scope) {
        return MemberRoleEntity.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .roleId(roleId)
                .scopeId(scope == null ? null : scope.getScopeId())
                .resourceId(scope == null ? null : scope.getResourceId())
                .active(true)
                .build();
    }

    private <T> Collection<T> collectionOrEmpty(Collection<T> collection) {
        return collection == null ? List.of() : collection;
    }
}
