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
import dev.vality.orgmanager.entity.StoredInvitationStatus;
import dev.vality.orgmanager.service.dto.MemberWithRoleDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class AdminManagementConverter {

    private static final String NULL_METADATA = "null";

    public Organization toOrganization(OrganizationEntity entity) {
        Organization organization = new Organization(
                entity.getId(),
                entity.getParty(),
                entity.getOwner(),
                entity.getName(),
                formatDateTime(entity.getCreatedAt()),
                toOrganizationStatus(entity.getStatus()));
        String metadata = metadataOrNull(entity.getMetadata());
        if (metadata != null) {
            organization.setMetadata(metadata);
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

    /**
     * Собирает участников из плоской выборки {@code member x member_role}. Строки без
     * memberRoleId — участники без активных ролей в организации, у них список ролей пустой.
     */
    public List<Member> toMembers(List<MemberWithRoleDto> rows) {
        Map<String, Member> members = new LinkedHashMap<>();
        for (MemberWithRoleDto row : collectionOrEmpty(rows)) {
            Member member = members.computeIfAbsent(row.getId(), id -> {
                Member created = new Member(id, new ArrayList<>());
                if (row.getEmail() != null) {
                    created.setEmail(row.getEmail());
                }
                return created;
            });
            if (row.getMemberRoleId() != null) {
                member.getRoles().add(toMemberRole(row));
            }
        }
        return List.copyOf(members.values());
    }

    public MemberRole toMemberRole(MemberRoleEntity entity) {
        MemberRole role = new MemberRole(entity.getId(), entity.getRoleId());
        if (entity.getScopeId() != null) {
            role.setScope(toRoleScope(entity.getScopeId(), entity.getResourceId()));
        }
        return role;
    }

    private MemberRole toMemberRole(MemberWithRoleDto row) {
        MemberRole role = new MemberRole(row.getMemberRoleId(), row.getRoleId());
        if (row.getScopeId() != null) {
            role.setScope(toRoleScope(row.getScopeId(), row.getResourceId()));
        }
        return role;
    }

    public RoleAssignment toRoleAssignment(MemberRoleEntity entity) {
        RoleAssignment assignment = new RoleAssignment(entity.getRoleId());
        if (entity.getScopeId() != null) {
            assignment.setScope(toRoleScope(entity.getScopeId(), entity.getResourceId()));
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
        String metadata = metadataOrNull(entity.getMetadata());
        if (metadata != null) {
            invitation.setMetadata(metadata);
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

    private RoleScope toRoleScope(String scopeId, String resourceId) {
        RoleScope scope = new RoleScope(scopeId);
        if (resourceId != null) {
            scope.setResourceId(resourceId);
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
        return switch (StoredInvitationStatus.fromValue(status)) {
            case PENDING -> InvitationStatus.pending;
            case ACCEPTED -> InvitationStatus.accepted;
            case EXPIRED -> InvitationStatus.expired;
            case REVOKED -> InvitationStatus.revoked;
        };
    }


    private String metadataOrNull(String metadata) {
        return metadata == null || NULL_METADATA.equals(metadata.strip()) ? null : metadata;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC).toString();
    }

    private <T> Collection<T> collectionOrEmpty(Collection<T> collection) {
        return collection == null ? List.of() : collection;
    }
}
