package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.InvalidRequest;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.RoleScope;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.exception.InvalidAllowedIpException;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.util.AllowedIps;
import dev.vality.orgmanager.util.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCommonService {

    public static final int DEFAULT_PAGE_LIMIT = 20;
    public static final int MAX_PAGE_LIMIT = 1000;

    private final OrganizationRepository organizationRepository;
    private final JsonCodec jsonCodec;

    public OrganizationEntity findOrganization(String organizationId) throws OrganizationNotFound {
        return organizationRepository.findById(organizationId)
                .orElseThrow(OrganizationNotFound::new);
    }

    public OrganizationEntity lockOrganization(String organizationId) throws OrganizationNotFound {
        organizationRepository.lockById(organizationId).orElseThrow(OrganizationNotFound::new);
        return findOrganization(organizationId);
    }

    public String toStoredMetadata(String metadata) throws InvalidRequest {
        if (metadata == null) {
            return null;
        }
        try {
            return jsonCodec.toJson(jsonCodec.toMap(metadata));
        } catch (RuntimeException exception) {
            throw new InvalidRequest("Metadata is expected to be a JSON object");
        }
    }

    public Set<String> toAllowedIps(Set<String> allowedIps) throws InvalidRequest {
        Set<String> normalized = AllowedIps.normalize(allowedIps);
        try {
            AllowedIps.validate(normalized);
            return normalized;
        } catch (InvalidAllowedIpException exception) {
            throw new InvalidRequest(exception.getMessage());
        }
    }

    public String requireText(String value, String field) throws InvalidRequest {
        if (value == null || value.isBlank()) {
            throw new InvalidRequest(field + " must not be blank");
        }
        return value;
    }

    /**
     * Область действия без resource_id не ограничивает роль, поэтому такое назначение отклоняется.
     */
    public void validateRoleAssignment(String roleId, RoleScope scope) throws InvalidRequest {
        requireText(roleId, "Role id");
        if (scope != null) {
            requireText(scope.getScopeId(), "Scope id");
            requireText(scope.getResourceId(), "Scope resource id");
        }
    }

    public MemberRoleEntity toMemberRoleEntity(String organizationId, String roleId, RoleScope scope) {
        return MemberRoleEntity.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .roleId(roleId)
                .scopeId(scope == null ? null : scope.getScopeId())
                .resourceId(scope == null ? null : scope.getResourceId())
                .active(true)
                .build();
    }

    public static <T> Collection<T> collectionOrEmpty(Collection<T> collection) {
        return collection == null ? List.of() : collection;
    }

    public static int pageLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_LIMIT;
        }
        return Math.min(requested, MAX_PAGE_LIMIT);
    }
}
