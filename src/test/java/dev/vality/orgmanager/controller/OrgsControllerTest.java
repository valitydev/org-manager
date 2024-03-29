package dev.vality.orgmanager.controller;

import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.exception.AccessDeniedException;
import dev.vality.orgmanager.exception.BouncerException;
import dev.vality.orgmanager.service.dto.ResourceDto;
import dev.vality.orgmanager.util.TestData;
import dev.vality.swag.organizations.model.InvitationRequest;
import dev.vality.swag.organizations.model.MemberRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsAnything.anything;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrgsControllerTest extends AbstractControllerTest {

    public static final String ORGANIZATION_ID = "3Kf21K54ldE3";

    public static final String MEMBER_ID = "L6Mc2la1D9Rg";

    @Test
    void expelOrgMemberWithErrorCallBouncer() throws Exception {
        doThrow(new BouncerException("Error bouncer", new RuntimeException())).when(resourceAccessService)
                .checkRights(ArgumentMatchers.any(ResourceDto.class));

        mockMvc.perform(delete(String.format("/orgs/%s/members/%s", ORGANIZATION_ID, MEMBER_ID))
                .contentType("application/json")
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void expelOrgMemberWithoutAccess() throws Exception {
        doThrow(new AccessDeniedException("Access denied")).when(resourceAccessService)
                .checkRights(ArgumentMatchers.any(ResourceDto.class));

        mockMvc.perform(delete(String.format("/orgs/%s/members/%s", ORGANIZATION_ID, MEMBER_ID))
                .contentType("application/json")
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
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
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
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
                        .header("Authorization", "Bearer " + generateRbkAdminJwt())
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
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
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
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
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
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
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
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
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
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
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
                .header("Authorization", "Bearer " + generateRbkAdminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", anything()));
    }

}
