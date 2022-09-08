package dev.vality.orgmanager.converter;

import dev.vality.bouncer.base.Entity;
import dev.vality.bouncer.context.v1.OrgRole;
import dev.vality.bouncer.context.v1.OrgRoleScope;
import dev.vality.bouncer.context.v1.User;
import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.swag.organizations.model.RoleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BouncerContextConverterTest {

    private BouncerContextConverter converter;

    @BeforeEach
    void setUp() {
        converter = new BouncerContextConverter();
    }

    @Test
    void shouldConvertToOrgRole() {
        MemberRoleEntity entity = MemberRoleEntity.builder()
                .id("id")
                .roleId("Administrator")
                .scopeId("Shop")
                .resourceId("resource")
                .organizationId("org")
                .build();

        OrgRole actual = converter.toOrgRole(entity);

        OrgRole expected = new OrgRole()
                .setId(RoleId.ADMINISTRATOR.getValue())
                .setScope(new OrgRoleScope()
                        .setShop(new Entity().setId("resource")));

        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertToMember() {
        Set<OrganizationEntity> organizationEntities = Set.of(TestObjectFactory.buildOrganization());
        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        memberEntity.setOrganizations(organizationEntities);

        User user = converter.toUser(memberEntity, organizationEntities);

        assertEquals(memberEntity.getId(), user.getId());
        assertEquals(memberEntity.getEmail(), user.getEmail());
        assertEquals(memberEntity.getOrganizations().size(), user.getOrgs().size());
    }

    @Test
    void shouldConvertToOrganizationWithoutRoles() {
        OrganizationEntity organizationEntity = TestObjectFactory.buildOrganization();

        var organization = converter.toOrganization(organizationEntity, Collections.emptySet());

        assertEquals(organizationEntity.getId(), organization.getId());
        assertEquals(organizationEntity.getOwner(), organization.getOwner().getId());
        assertNull(organization.getRoles());
    }

    @Test
    void shouldConvertToOrganizationWithoutRolesAnotherOrganization() {
        OrganizationEntity organizationEntity = TestObjectFactory.buildOrganization();
        MemberRoleEntity memberRoleEntity =
                TestObjectFactory.buildMemberRole(RoleId.ADMINISTRATOR, TestObjectFactory.randomString());

        var organization = converter.toOrganization(organizationEntity, Set.of(memberRoleEntity));

        assertEquals(organizationEntity.getId(), organization.getId());
        assertEquals(organizationEntity.getOwner(), organization.getOwner().getId());
        assertEquals(organizationEntity.getParty(), organization.getParty().getId());
        assertTrue(organization.getRoles().isEmpty());
    }

    @Test
    void shouldConvertToOrganization() {
        OrganizationEntity organizationEntity = TestObjectFactory.buildOrganization();
        MemberRoleEntity memberRoleEntity =
                TestObjectFactory.buildMemberRole(RoleId.ADMINISTRATOR, organizationEntity.getId());

        var organization = converter.toOrganization(organizationEntity, Set.of(memberRoleEntity));

        assertEquals(organizationEntity.getId(), organization.getId());
        assertEquals(organizationEntity.getOwner(), organization.getOwner().getId());
        assertEquals(organizationEntity.getParty(), organization.getParty().getId());
        assertEquals(memberRoleEntity.getRoleId(), organization.getRoles().iterator().next().getId());
    }
}
