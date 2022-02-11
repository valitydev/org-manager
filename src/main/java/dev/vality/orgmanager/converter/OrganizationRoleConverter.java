package dev.vality.orgmanager.converter;

import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.swag.organizations.model.ResourceScopeId;
import dev.vality.swag.organizations.model.Role;
import dev.vality.swag.organizations.model.RoleId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationRoleConverter {

    public Role toDomain(OrganizationRoleEntity entity) {
        return new Role()
                .id(RoleId.fromValue(entity.getRoleId()))
                .name(entity.getName())
                .scopes(entity.getPossibleScopes()
                        .stream()
                        .map(s -> ResourceScopeId.fromValue(s.getId()))
                        .collect(Collectors.toList()));
    }
}