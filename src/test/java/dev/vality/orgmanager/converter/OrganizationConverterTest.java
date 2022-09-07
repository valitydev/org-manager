package dev.vality.orgmanager.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.util.JsonMapper;
import dev.vality.swag.organizations.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganizationConverterTest {

    private OrganizationConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new OrganizationConverter(
                new JsonMapper(
                        new ObjectMapper()));
    }

    @Test
    void shouldConvertToEntity() {
        // Given
        Organization organization = new Organization()
                .name(TestObjectFactory.randomString())
                .metadata(Map.of("a", "b"));

        // When
        String testOwnerId = "testOwnerId";
        OrganizationEntity entity = converter.toEntity(organization, testOwnerId);

        // Then
        assertThat(entity.getId()).isNotEmpty();
        assertThat(entity.getParty()).isNotEmpty();
        assertEquals(entity.getId(), entity.getParty());
        assertThat(entity.getCreatedAt()).isNotNull();
        assertEquals(organization.getName(), entity.getName());
        assertEquals("{\"a\":\"b\"}", entity.getMetadata());
        assertEquals(testOwnerId, entity.getOwner());
    }

    @Test
    void shouldConvertToDomain() {
        // Given
        OrganizationEntity entity = TestObjectFactory.buildOrganization();

        // When
        Organization organization = converter.toDomain(entity);

        // Then
        assertThat(organization.getId()).isNotEmpty();
        assertThat(organization.getParty()).isNotEmpty();
        assertThat(organization.getCreatedAt()).isNotNull();
        assertEquals(entity.getName(), organization.getName());
        assertEquals(Map.of("a", "b"), organization.getMetadata());
        assertEquals(entity.getOwner(), organization.getOwner());
    }
}
