package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.RoleScope;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.util.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCommonService {

    private final OrganizationRepository organizationRepository;
    private final JsonCodec jsonCodec;

    public OrganizationEntity findOrganization(String organizationId) throws OrganizationNotFound {
        return organizationRepository.findById(organizationId)
                .orElseThrow(OrganizationNotFound::new);
    }

    public String toStoredMetadata(String metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return jsonCodec.toJson(jsonCodec.toMap(metadata));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Metadata is expected to be a JSON object", exception);
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
}
