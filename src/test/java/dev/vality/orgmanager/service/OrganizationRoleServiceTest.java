package dev.vality.orgmanager.service;

import dev.vality.orgmanager.converter.OrganizationRoleConverter;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.orgmanager.exception.ResourceNotFoundException;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.repository.OrganizationRoleRepository;
import dev.vality.swag.organizations.model.Role;
import dev.vality.swag.organizations.model.RoleAvailableListResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrganizationRoleServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationRoleRepository organizationRoleRepository;
    @Mock private OrganizationRoleConverter organizationRoleConverter;

    @InjectMocks
    private OrganizationRoleService service;

    @Test
    void shouldListRoles() {
        // Given
        OrganizationRoleEntity organizationRoleEntity = new OrganizationRoleEntity();
        Role role = new Role();

        String orgId = "orgId";
        OrganizationEntity organizationEntity = OrganizationEntity.builder()
                .roles(Set.of(organizationRoleEntity))
                .build();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(organizationEntity));
        when(organizationRoleConverter.toDomain(organizationRoleEntity))
                .thenReturn(role);

        // When
        ResponseEntity<RoleAvailableListResult> response = service.list(orgId);

        // Then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull();
        assertThat(response.getBody().getResult())
                .containsExactly(role);
    }

    @Test
    void shouldReturnNotFoundIfNoOrganizationExistForRolesList() {
        // Given
        String orgId = "orgId";

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.empty());

        // When
        ResponseEntity<RoleAvailableListResult> response = service.list(orgId);

        // Then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isNull();
    }

    @Test
    void shouldFindRoleById() {
        OrganizationRoleEntity organizationRoleEntity = new OrganizationRoleEntity();
        Role role = new Role();
        String orgId = "orgId";
        String roleId = "Administrator";
        when(organizationRepository.existsById(orgId))
                .thenReturn(true);
        when(organizationRoleRepository.findByOrganizationIdAndRoleId(orgId, roleId))
                .thenReturn(Optional.of(organizationRoleEntity));
        when(organizationRoleConverter.toDomain(organizationRoleEntity))
                .thenReturn(role);

        Role response = service.get(orgId, roleId);

        assertThat(response)
                .isEqualTo(role);
    }

    @Test
    void shouldReturnNotFoundIfOrganizationDoesNotExist() {
        String orgId = "orgId";
        String roleId = "Administrator";
        when(organizationRepository.existsById(orgId))
                .thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.get(orgId, roleId));

    }
}
