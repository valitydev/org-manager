package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.InvalidRequest;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.RoleScope;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.orgmanager.entity.ScopeEntity;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.repository.OrganizationRoleRepository;
import dev.vality.orgmanager.util.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCommonService {

    public static final int DEFAULT_PAGE_LIMIT = 20;
    public static final int MAX_PAGE_LIMIT = 1000;

    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final JsonCodec jsonCodec;

    public OrganizationEntity findOrganization(String organizationId) throws OrganizationNotFound {
        return organizationRepository.findById(organizationId)
                .orElseThrow(OrganizationNotFound::new);
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

    public String requireText(String value, String field) throws InvalidRequest {
        if (value == null || value.isBlank()) {
            throw new InvalidRequest(field + " must not be blank");
        }
        return value;
    }

    /**
     * Проверяет, что назначение роли опирается на каталог ролей организации: сама роль в нём есть,
     * а указанный scope_id входит в её список допустимых областей действия.
     */
    public void validateRoleAssignment(String organizationId, String roleId, RoleScope scope)
            throws InvalidRequest {
        requireText(roleId, "Role id");
        OrganizationRoleEntity role = organizationRoleRepository
                .findByOrganizationIdAndRoleId(organizationId, roleId)
                .orElseThrow(() -> new InvalidRequest(
                        "Role " + roleId + " is not present in the organization role catalog"));
        if (scope == null) {
            return;
        }
        String scopeId = requireText(scope.getScopeId(), "Scope id");
        if (!allowedScopeIds(role).contains(scopeId)) {
            throw new InvalidRequest("Scope " + scopeId + " is not allowed for role " + roleId);
        }
    }

    public Set<String> allowedScopeIds(OrganizationRoleEntity role) {
        return collectionOrEmpty(role.getPossibleScopes()).stream()
                .map(ScopeEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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
