package dev.vality.orgmanager.converter;

import dev.vality.orgmanagement.Invitation;
import dev.vality.orgmanagement.InvitationStatus;
import dev.vality.orgmanagement.Member;
import dev.vality.orgmanagement.MemberRole;
import dev.vality.orgmanagement.Organization;
import dev.vality.orgmanagement.OrganizationRole;
import dev.vality.orgmanagement.OrganizationStatus;
import dev.vality.orgmanagement.RoleAssignment;
import dev.vality.orgmanagement.RoleScope;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class AdminManagementConverter {

    public Organization toOrganization(OrganizationEntity entity) {
        Organization organization = new Organization(
                entity.getId(),
                entity.getParty(),
                entity.getOwner(),
                entity.getName(),
                formatDateTime(entity.getCreatedAt()),
                toOrganizationStatus(entity.getStatus()));
        if (entity.getMetadata() != null) {
            organization.setMetadata(entity.getMetadata());
        }
        return organization;
    }

    public Member toMember(MemberEntity entity, String organizationId) {
        List<MemberRole> roles = collectionOrEmpty(entity.getRoles()).stream()
                .filter(MemberRoleEntity::isActive)
                .filter(role -> organizationId.equals(role.getOrganizationId()))
                .sorted(Comparator.comparing(MemberRoleEntity::getId))
                .map(this::toMemberRole)
                .toList();
        Member member = new Member(entity.getId(), roles);
        if (entity.getEmail() != null) {
            member.setEmail(entity.getEmail());
        }
        return member;
    }

    public MemberRole toMemberRole(MemberRoleEntity entity) {
        MemberRole role = new MemberRole(entity.getId(), entity.getRoleId());
        if (entity.getScopeId() != null) {
            role.setScope(toRoleScope(entity));
        }
        return role;
    }

    public RoleAssignment toRoleAssignment(MemberRoleEntity entity) {
        RoleAssignment assignment = new RoleAssignment(entity.getRoleId());
        if (entity.getScopeId() != null) {
            assignment.setScope(toRoleScope(entity));
        }
        return assignment;
    }

    public Invitation toInvitation(InvitationEntity entity) {
        List<RoleAssignment> roles = collectionOrEmpty(entity.getInviteeRoles()).stream()
                .sorted(Comparator.comparing(MemberRoleEntity::getId))
                .map(this::toRoleAssignment)
                .toList();
        Invitation invitation = new Invitation(
                entity.getId(),
                entity.getOrganizationId(),
                formatDateTime(entity.getCreatedAt()),
                formatDateTime(entity.getExpiresAt()),
                entity.getInviteeContactEmail(),
                roles,
                effectiveInvitationStatus(entity));
        if (entity.getMetadata() != null) {
            invitation.setMetadata(entity.getMetadata());
        }
        if (entity.getAcceptedAt() != null) {
            invitation.setAcceptedAt(formatDateTime(entity.getAcceptedAt()));
        }
        if (entity.getAcceptedMemberId() != null) {
            invitation.setAcceptedMemberId(entity.getAcceptedMemberId());
        }
        if (entity.getRevokedAt() != null) {
            invitation.setRevokedAt(formatDateTime(entity.getRevokedAt()));
        }
        if (entity.getRevocationReason() != null) {
            invitation.setRevocationReason(entity.getRevocationReason());
        }
        return invitation;
    }

    public InvitationStatus effectiveInvitationStatus(InvitationEntity entity) {
        InvitationStatus storedStatus = toInvitationStatus(entity.getStatus());
        if (storedStatus == InvitationStatus.pending && entity.isExpired()) {
            return InvitationStatus.expired;
        }
        return storedStatus;
    }

    public OrganizationRole toOrganizationRole(OrganizationRoleEntity entity) {
        List<String> scopeIds = collectionOrEmpty(entity.getPossibleScopes()).stream()
                .map(scope -> scope.getId())
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        return new OrganizationRole(entity.getRoleId(), entity.getName(), scopeIds);
    }

    private RoleScope toRoleScope(MemberRoleEntity entity) {
        RoleScope scope = new RoleScope(entity.getScopeId());
        if (entity.getResourceId() != null) {
            scope.setResourceId(entity.getResourceId());
        }
        return scope;
    }

    private OrganizationStatus toOrganizationStatus(String status) {
        if (status == null) {
            return OrganizationStatus.active;
        }
        return OrganizationStatus.valueOf(status.toLowerCase(Locale.ROOT));
    }

    private InvitationStatus toInvitationStatus(String status) {
        return InvitationStatus.valueOf(status.toLowerCase(Locale.ROOT));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC).toString();
    }

    private <T> Collection<T> collectionOrEmpty(Collection<T> collection) {
        return collection == null ? List.of() : collection;
    }
}
