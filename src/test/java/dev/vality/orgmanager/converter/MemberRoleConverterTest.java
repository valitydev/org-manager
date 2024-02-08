package dev.vality.orgmanager.converter;

import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.swag.organizations.model.MemberRole;
import dev.vality.swag.organizations.model.MemberRoleScope;
import dev.vality.swag.organizations.model.ResourceScopeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberRoleConverterTest {

    private MemberRoleConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new MemberRoleConverter();
    }

    @Test
    void shouldConvertToEntityWithoutScope() {
        // Given
        MemberRole role = new MemberRole()
                .roleId("Administrator");

        // When
        MemberRoleEntity entity = converter.toEntity(role, "org");

        // Then
        MemberRoleEntity expected = MemberRoleEntity.builder()
                .roleId("Administrator")
                .organizationId("org")
                .active(true)
                .build();

        assertThat(entity.getId()).isNotEmpty();
        assertThat(entity).usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expected);
    }

    @Test
    void shouldConvertToEntity() {
        // Given
        MemberRole role = new MemberRole()
                .roleId("Administrator")
                .scope(new MemberRoleScope()
                        .resourceId("resource")
                        .id(ResourceScopeId.SHOP));

        // When
        MemberRoleEntity entity = converter.toEntity(role, "org");

        // Then
        MemberRoleEntity expected = MemberRoleEntity.builder()
                .roleId("Administrator")
                .scopeId("Shop")
                .resourceId("resource")
                .organizationId("org")
                .active(true)
                .build();

        assertThat(entity.getId()).isNotEmpty();
        assertThat(entity)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expected);
    }

    @Test
    void shouldConvertToDomainWithoutScope() {
        // Given
        MemberRoleEntity entity = MemberRoleEntity.builder()
                .id("id")
                .roleId("Administrator")
                .organizationId("org")
                .build();

        // When
        MemberRole role = converter.toDomain(entity);

        // Then
        MemberRole expected = new MemberRole()
                .id(entity.getId())
                .roleId("Administrator");

        assertThat(role).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldConvertToDomain() {
        // Given
        MemberRoleEntity entity = MemberRoleEntity.builder()
                .id("id")
                .roleId("Administrator")
                .scopeId("Shop")
                .resourceId("resource")
                .organizationId("org")
                .build();

        // When
        MemberRole role = converter.toDomain(entity);

        // Then
        MemberRole expected = new MemberRole()
                .id(entity.getId())
                .roleId("Administrator")
                .scope(new MemberRoleScope()
                        .resourceId("resource")
                        .id(ResourceScopeId.SHOP));

        assertThat(role).usingRecursiveComparison().isEqualTo(expected);
    }
}