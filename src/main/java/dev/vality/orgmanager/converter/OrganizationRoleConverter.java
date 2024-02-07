package dev.vality.orgmanager.converter;

import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.swag.organizations.model.ResourceScopeId;
import dev.vality.swag.organizations.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationRoleConverter {

    public Role toDomain(OrganizationRoleEntity entity) {
        return new Role()
                .id(entity.getRoleId())
                .name(entity.getName())
                .scopes(entity.getPossibleScopes()
                        .stream()
                        .map(s -> ResourceScopeId.fromValue(s.getId()))
                        .toList());
    }
}