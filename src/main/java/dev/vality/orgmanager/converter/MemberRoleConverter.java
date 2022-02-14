package dev.vality.orgmanager.converter;

import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.service.dto.MemberWithRoleDto;
import dev.vality.swag.organizations.model.MemberRole;
import dev.vality.swag.organizations.model.MemberRoleScope;
import dev.vality.swag.organizations.model.ResourceScopeId;
import dev.vality.swag.organizations.model.RoleId;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class MemberRoleConverter {

    public MemberRoleEntity toEntity(MemberRole role, String orgId) {
        MemberRoleScope scope = role.getScope();
        return MemberRoleEntity.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(orgId)
                .resourceId(Objects.nonNull(scope) ? scope.getResourceId() : null)
                .roleId(role.getRoleId().toString())
                .scopeId(Objects.nonNull(scope) ? scope.getId().toString() : null)
                .active(Boolean.TRUE)
                .build();
    }

    public MemberRole toDomain(MemberRoleEntity entity) {
        MemberRoleScope memberRoleScope = Optional.ofNullable(entity.getScopeId())
                .map(ResourceScopeId::fromValue)
                .map(resourceScopeId -> new MemberRoleScope()
                        .id(resourceScopeId)
                        .resourceId(entity.getResourceId()))
                .orElse(null);
        return new MemberRole()
                .id(entity.getId())
                .roleId(RoleId.fromValue(entity.getRoleId()))
                .scope(memberRoleScope);
    }

    public MemberRole toDomain(MemberWithRoleDto memberWithRoleDto) {
        MemberRoleScope memberRoleScope = Optional.ofNullable(memberWithRoleDto.getScopeId())
                .map(ResourceScopeId::fromValue)
                .map(resourceScopeId -> new MemberRoleScope()
                        .id(resourceScopeId)
                        .resourceId(memberWithRoleDto.getResourceId()))
                .orElse(null);
        return new MemberRole()
                .id(memberWithRoleDto.getMemberRoleId())
                .roleId(RoleId.fromValue(memberWithRoleDto.getRoleId()))
                .scope(memberRoleScope);
    }
}
