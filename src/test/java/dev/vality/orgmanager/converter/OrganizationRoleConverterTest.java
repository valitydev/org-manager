package dev.vality.orgmanager.converter;

import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.orgmanager.entity.ScopeEntity;
import dev.vality.swag.organizations.model.ResourceScopeId;
import dev.vality.swag.organizations.model.Role;
import dev.vality.swag.organizations.model.RoleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationRoleConverterTest {

    private OrganizationRoleConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new OrganizationRoleConverter();
    }

    @Test
    void shouldConvertToDomain() {
        // Given
        OrganizationRoleEntity entity = OrganizationRoleEntity.builder()
                .id("id")
                .organizationId("orgId")
                .name("name")
                .roleId("Administrator")
                .possibleScopes(Set.of(ScopeEntity.builder()
                        .id("Shop")
                        .build()))
                .build();

        // When
        Role role = converter.toDomain(entity);

        // Then
        Role expected = new Role()
                .id(RoleId.ADMINISTRATOR)
                .name("name")
                .scopes(List.of(ResourceScopeId.SHOP));

        assertThat(role).isEqualToComparingFieldByField(expected);
    }
}
