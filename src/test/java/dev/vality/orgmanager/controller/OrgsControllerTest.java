package dev.vality.orgmanager.controller;

import dev.vality.orgmanagement.ModifyOrganizationRequest;
import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.exception.AccessDeniedException;
import dev.vality.orgmanager.exception.BouncerException;
import dev.vality.orgmanager.service.AdminManagementService;
import dev.vality.orgmanager.service.dto.ResourceDto;
import dev.vality.orgmanager.util.TestData;
import dev.vality.swag.organizations.model.InvitationRequest;
import dev.vality.swag.organizations.model.MemberRole;
import dev.vality.swag.organizations.model.Organization;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsAnything.anything;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrgsControllerTest extends AbstractControllerTest {

    public static final String ORGANIZATION_ID = "3Kf21K54ldE3";

    public static final String MEMBER_ID = "L6Mc2la1D9Rg";

    @Autowired
    private AdminManagementService adminManagementService;

    @Test
    void expelOrgMemberWithErrorCallBouncer() throws Exception {
        doThrow(new BouncerException("Error bouncer", new RuntimeException())).when(resourceAccessService)
                .checkRights(ArgumentMatchers.any(ResourceDto.class));

        mockMvc.perform(delete(String.format("/orgs/%s/members/%s", ORGANIZATION_ID, MEMBER_ID))
                .contentType("application/json")
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void expelOrgMemberWithoutAccess() throws Exception {
        doThrow(new AccessDeniedException("Access denied")).when(resourceAccessService)
                .checkRights(ArgumentMatchers.any(ResourceDto.class));

        mockMvc.perform(delete(String.format("/orgs/%s/members/%s", ORGANIZATION_ID, MEMBER_ID))
                .contentType("application/json")
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignMemberRoleWithoutAccess() throws Exception {
        MemberRole memberRole = TestData.buildMemberRole();
        doThrow(new AccessDeniedException("Access denied")).when(resourceAccessService)
                .checkRights(ArgumentMatchers.any(ResourceDto.class));

        mockMvc.perform(post(String.format("/orgs/%s/members/%s/roles", ORGANIZATION_ID, MEMBER_ID))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(memberRole))
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignMemberRoleTest() throws Exception {
        OrganizationEntity organizationEntity = TestData.buildOrganization(ORGANIZATION_ID, MEMBER_ID);
        organizationRepository.save(organizationEntity);

        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(memberEntity);
        MemberRoleEntity memberRoleEntity = TestObjectFactory.buildMemberRole("Accountant", organization.getId());
        MemberRoleEntity savedMemberRole = memberRoleRepository.save(
                memberRoleEntity);
        memberEntity.setRoles(Set.of(savedMemberRole));
        MemberEntity savedMember = memberRepository.save(memberEntity);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        MemberRole memberRole = TestData.buildMemberRole();

        mockMvc.perform(post(String.format("/orgs/%s/members/%s/roles", savedOrganization.getId(), savedMember.getId()))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(memberRole))
                        .header("Authorization", "Bearer " + generateAdminJwt())
                        .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.roleId", equalTo(memberRole.getRoleId())))
                .andExpect(jsonPath("$.scope.id", equalTo(memberRole.getScope().getId().getValue())))
                .andExpect(jsonPath("$.scope.resourceId", equalTo(memberRole.getScope().getResourceId())));

        assertFalse(memberRoleRepository.findAll().isEmpty());
    }

    @Test
    @Transactional
    void expelOrgMemberTest() throws Exception {
        MemberEntity member = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(member);
        MemberRoleEntity savedMemberRole =
                memberRoleRepository.save(TestObjectFactory.buildMemberRole("Accountant", organization.getId()));
        member.setRoles(Set.of(savedMemberRole));
        MemberEntity savedMember = memberRepository.save(member);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);


        mockMvc.perform(delete(String.format("/orgs/%s/members/%s", savedOrganization.getId(), savedMember.getId()))
                .contentType("application/json")
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isNoContent());

        OrganizationEntity organizationEntity = organizationRepository.findById(savedOrganization.getId()).get();
        assertTrue(organizationEntity.getMembers().stream().noneMatch(m -> m.getId().equals(savedMember.getId())));
        MemberEntity memberEntity = memberRepository.findById(savedMember.getId()).get();
        assertTrue(memberEntity.getRoles().isEmpty());
    }

    @Test
    @Transactional
    void removeMemberRoleWithOnlyOneRole() throws Exception {
        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(memberEntity);
        MemberRoleEntity memberRoleEntity = TestObjectFactory.buildMemberRole("Accountant", organization.getId());
        MemberRoleEntity savedMemberRole = memberRoleRepository.save(
                memberRoleEntity);
        memberEntity.setRoles(Set.of(savedMemberRole));
        MemberEntity savedMember = memberRepository.save(memberEntity);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        mockMvc.perform(delete(
                String.format("/orgs/%s/members/%s/roles/%s", savedOrganization.getId(), savedMember.getId(),
                        savedMemberRole.getId())
        )
                .contentType("application/json")
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    void removeMemberRole() throws Exception {
        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(memberEntity);
        MemberRoleEntity memberRoleEntity = TestObjectFactory.buildMemberRole("Accountant", organization.getId());
        MemberRoleEntity roleToRemove = TestObjectFactory.buildMemberRole("Manager", organization.getId());
        List<MemberRoleEntity> roles = memberRoleRepository.saveAll(List.of(
                memberRoleEntity, roleToRemove));
        memberEntity.setRoles(new HashSet<>(roles));
        MemberEntity savedMember = memberRepository.save(memberEntity);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        mockMvc.perform(delete(
                        String.format("/orgs/%s/members/%s/roles/%s", savedOrganization.getId(), savedMember.getId(),
                                roleToRemove.getId())
                )
                .contentType("application/json")
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isNoContent());


        assertThat(memberRepository.findById(savedMember.getId()).get().getRoles(), not(hasItem(roleToRemove)));
    }

    @Test
    void createInvitationWithoutAccess() throws Exception {
        OrganizationEntity organizationEntity = TestData.buildOrganization(ORGANIZATION_ID, MEMBER_ID);
        organizationRepository.save(organizationEntity);
        InvitationRequest invitation = TestData.buildInvitationRequest();
        String body = objectMapper.writeValueAsString(invitation);

        doThrow(new AccessDeniedException("Access denied")).when(resourceAccessService)
                .checkRights(ArgumentMatchers.any(ResourceDto.class));

        mockMvc.perform(post(String.format("/orgs/%s/invitations", ORGANIZATION_ID))
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInvitationTest() throws Exception {
        OrganizationEntity organizationEntity = TestData.buildOrganization(ORGANIZATION_ID, MEMBER_ID);
        organizationRepository.save(organizationEntity);
        InvitationRequest invitation = TestData.buildInvitationRequest();
        String body = objectMapper.writeValueAsString(invitation);

        mockMvc.perform(post(String.format("/orgs/%s/invitations", ORGANIZATION_ID))
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(jsonPath("$.status", is("Pending")))
                .andExpect(jsonPath("$.acceptToken").doesNotExist());
    }

    @Test
    void listOrgMembersTest() throws Exception {
        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        MemberEntity savedMember = memberRepository.save(
                memberEntity);
        OrganizationEntity organizationEntity = TestObjectFactory.buildOrganization(savedMember);
        OrganizationEntity savedOrganization = organizationRepository.save(
                organizationEntity);

        mockMvc.perform(get(String.format("/orgs/%s/members", savedOrganization.getId()))
                .contentType("application/json")
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", anything()));
    }


    @Test
    void createOrgWithAllowedIps() throws Exception {
        doNothing().when(resourceAccessService).checkRights();
        Organization organization = new Organization()
                .name("Organization")
                .allowedIps(Set.of(" 1.2.3.4", "10.0.0.1"));

        String orgId = objectMapper.readTree(mockMvc.perform(post("/orgs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(organization))
                        .header("Authorization", "Bearer " + generateAdminJwt())
                        .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allowedIps", containsInAnyOrder("1.2.3.4", "10.0.0.1")))
                .andReturn().getResponse().getContentAsString()).get("id").asString();

        assertThat(organizationRepository.findById(orgId).orElseThrow().getAllowedIps(),
                is(Set.of("1.2.3.4", "10.0.0.1")));
    }

    @Test
    void createOrgWithInvalidAllowedIps() throws Exception {
        doNothing().when(resourceAccessService).checkRights();
        Organization organization = new Organization()
                .name("Organization")
                .allowedIps(Set.of("1.2.3.4", "not-an-ip"));

        mockMvc.perform(post("/orgs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(organization))
                        .header("Authorization", "Bearer " + generateAdminJwt())
                        .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("invalidRequest")));

        assertTrue(organizationRepository.findAll().isEmpty());
    }

    @Test
    void patchOrgAllowedIps() throws Exception {
        OrganizationEntity organization = saveOrganizationWithAllowedIps("1.2.3.4");

        patchOrg(organization.getId(), "{\"allowedIps\": [\" 5.6.7.8\", \"::1\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo(organization.getName())))
                .andExpect(jsonPath("$.allowedIps", containsInAnyOrder("5.6.7.8", "::1")));

        assertThat(organizationRepository.findById(organization.getId()).orElseThrow().getAllowedIps(),
                is(Set.of("5.6.7.8", "::1")));
    }

    @Test
    void patchOrgNameKeepsAllowedIps() throws Exception {
        OrganizationEntity organization = saveOrganizationWithAllowedIps("1.2.3.4");

        patchOrg(organization.getId(), "{\"name\": \"Renamed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("Renamed")))
                .andExpect(jsonPath("$.allowedIps", containsInAnyOrder("1.2.3.4")));
    }

    @Test
    void patchOrgClearsAllowedIps() throws Exception {
        OrganizationEntity withEmptyList = saveOrganizationWithAllowedIps("1.2.3.4");
        OrganizationEntity withNull = saveOrganizationWithAllowedIps("1.2.3.4");

        patchOrg(withEmptyList.getId(), "{\"allowedIps\": []}").andExpect(status().isOk());
        patchOrg(withNull.getId(), "{\"allowedIps\": null}").andExpect(status().isOk());

        assertTrue(organizationRepository.findById(withEmptyList.getId()).orElseThrow().getAllowedIps().isEmpty());
        assertTrue(organizationRepository.findById(withNull.getId()).orElseThrow().getAllowedIps().isEmpty());
    }

    @Test
    void patchOrgWithInvalidAllowedIps() throws Exception {
        OrganizationEntity organization = saveOrganizationWithAllowedIps("1.2.3.4");

        patchOrg(organization.getId(), "{\"name\": \"Renamed\", \"allowedIps\": [\"not-an-ip\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("invalidRequest")));

        OrganizationEntity stored = organizationRepository.findById(organization.getId()).orElseThrow();
        assertThat(stored.getName(), is(organization.getName()));
        assertThat(stored.getAllowedIps(), is(Set.of("1.2.3.4")));
    }

    @Test
    void patchOrgKeepsConcurrentChanges() throws Exception {
        OrganizationEntity organization = saveOrganizationWithAllowedIps("1.2.3.4");
        String orgId = organization.getId();
        doAnswer(invocation -> {
            organizationRepository.findById(orgId);
            CompletableFuture.runAsync(() -> modifyAllowedIps(orgId, "5.6.7.8")).join();
            return null;
        }).when(resourceAccessService).checkRights(ArgumentMatchers.any(ResourceDto.class));

        mockMvc.perform(patch("/orgs/" + orgId)
                        .contentType("application/json")
                        .content("{\"name\": \"Renamed\"}")
                        .header("Authorization", "Bearer " + generateAdminJwt())
                        .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isOk());

        OrganizationEntity stored = organizationRepository.findById(orgId).orElseThrow();
        assertThat(stored.getName(), is("Renamed"));
        assertThat(stored.getAllowedIps(), is(Set.of("5.6.7.8")));
    }

    private void modifyAllowedIps(String orgId, String... allowedIps) {
        try {
            adminManagementService.modifyOrganization(
                    orgId, new ModifyOrganizationRequest().setAllowedIps(Set.of(allowedIps)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private OrganizationEntity saveOrganizationWithAllowedIps(String... allowedIps) {
        OrganizationEntity organization = TestObjectFactory.buildOrganization();
        organization.setAllowedIps(Set.of(allowedIps));
        return organizationRepository.save(organization);
    }

    private ResultActions patchOrg(String orgId, String body) throws Exception {
        doNothing().when(resourceAccessService).checkRights(ArgumentMatchers.any(ResourceDto.class));
        return mockMvc.perform(patch("/orgs/" + orgId)
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + generateAdminJwt())
                .header("X-Request-ID", "testRequestId"));
    }
}
