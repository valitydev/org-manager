package dev.vality.orgmanager.service;

import dev.vality.orgmanager.converter.OrganizationRoleConverter;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.orgmanager.exception.ResourceNotFoundException;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.repository.OrganizationRoleRepository;
import dev.vality.swag.organizations.model.Role;
import dev.vality.swag.organizations.model.RoleAvailableListResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrganizationRoleService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final OrganizationRoleConverter organizationRoleConverter;

    public Role get(String orgId, String roleId) {
        if (!organizationRepository.existsById(orgId)) {
            throw new ResourceNotFoundException();
        }
        OrganizationRoleEntity entity =
                organizationRoleRepository.findByOrganizationIdAndRoleId(orgId, roleId)
                        .orElseThrow(ResourceNotFoundException::new);

        return organizationRoleConverter.toDomain(entity);
    }

    public ResponseEntity<RoleAvailableListResult> list(String orgId) {
        Optional<OrganizationEntity> entity = organizationRepository.findById(orgId);

        if (entity.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        }

        List<Role> roles = entity.orElseThrow().getRoles()
                .stream()
                .map(organizationRoleConverter::toDomain)
                .toList();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RoleAvailableListResult()
                        .result(roles));
    }
}
