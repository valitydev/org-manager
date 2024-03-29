package dev.vality.orgmanager.service;


import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.exception.AccessDeniedException;
import dev.vality.orgmanager.exception.InviteAlreadyAcceptedException;
import dev.vality.orgmanager.exception.LastRoleException;
import dev.vality.orgmanager.repository.AbstractRepositoryTest;
import dev.vality.swag.organizations.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.vality.orgmanager.TestObjectFactory.*;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

public class OrganizationServiceIntegrationTest extends AbstractRepositoryTest {

    @Autowired
    private OrganizationService organizationService;

    @Test
    @Transactional
    void shouldGetOrgMember() {
        // Given
        MemberEntity member = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(member);
        MemberRoleEntity nonActiveRoleInOrg = buildMemberRole("Accountant", organization.getId());
        nonActiveRoleInOrg.setActive(Boolean.FALSE);
        MemberRoleEntity savedMemberRoleNonActiveInOrg = memberRoleRepository.save(nonActiveRoleInOrg);
        MemberRoleEntity activeRoleInOrg = buildMemberRole("Accountant", organization.getId());
        activeRoleInOrg.setActive(Boolean.TRUE);
        MemberRoleEntity savedMemberRoleInOrg =
                memberRoleRepository.save(activeRoleInOrg);
        MemberRoleEntity savedMemberRoleInAnotherOrg =
                memberRoleRepository
                        .save(TestObjectFactory.buildMemberRole("Accountant", TestObjectFactory.randomString()));
        member.setRoles(Set.of(savedMemberRoleNonActiveInOrg, savedMemberRoleInOrg, savedMemberRoleInAnotherOrg));
        MemberEntity savedMember = memberRepository.save(member);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        // When
        Member actualMember = organizationService.getOrgMember(savedMember.getId(), savedOrganization.getId());

        // Then
        assertEquals(savedMember.getId(), actualMember.getId());
        assertEquals(savedMember.getEmail(), actualMember.getUserEmail());
        List<String> roles = actualMember.getRoles().stream().map(MemberRole::getId).collect(toList());
        assertThat(roles, hasItem(savedMemberRoleInOrg.getId()));
        assertThat(roles, not(hasItem(savedMemberRoleInAnotherOrg.getId())));
        assertThat(roles, not(hasItem(savedMemberRoleNonActiveInOrg.getId())));
        assertEquals(3, memberRepository.findById(savedMember.getId()).get().getRoles().size());
    }

    @Test
    void shouldThrowAccessDeniedExceptionOnJoinOrgTest() {
        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(memberEntity);
        MemberRoleEntity memberRole = buildMemberRole("Accountant", organization.getId());
        memberRole.setActive(Boolean.FALSE);
        MemberRoleEntity savedMemberRole = memberRoleRepository.save(memberRole);
        memberEntity.setRoles(Set.of(savedMemberRole));
        MemberEntity savedMember = memberRepository.save(memberEntity);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);
        InvitationEntity savedInvitation =
                invitationRepository.save(buildInvitation(savedOrganization.getId(), randomString()));

        assertThrows(AccessDeniedException.class, () -> organizationService.joinOrganization(
                savedInvitation.getAcceptToken(),
                savedMember.getId(),
                savedMember.getEmail())
        );
    }

    @Test
    void shouldThrowInviteAlreadyAcceptedExceptionOnJoinOrgTest() {
        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity savedOrganization =
                organizationRepository.save(TestObjectFactory.buildOrganization(memberEntity));
        InvitationEntity alreadyAcceptedInvitation =
                buildInvitation(savedOrganization.getId(), TestObjectFactory.randomString());
        alreadyAcceptedInvitation.setAcceptedAt(LocalDateTime.now());
        alreadyAcceptedInvitation.setAcceptedMemberId(TestObjectFactory.randomString());
        alreadyAcceptedInvitation.setStatus(InvitationStatusName.ACCEPTED.getValue());
        invitationRepository.save(alreadyAcceptedInvitation);
        MemberRoleEntity memberRole = buildMemberRole("Accountant", savedOrganization.getId());
        memberRole.setActive(Boolean.FALSE);
        MemberRoleEntity savedMemberRole = memberRoleRepository.save(memberRole);
        memberEntity.setRoles(Set.of(savedMemberRole));
        MemberEntity savedMember = memberRepository.save(memberEntity);

        assertThrows(InviteAlreadyAcceptedException.class, () -> organizationService.joinOrganization(
                alreadyAcceptedInvitation.getAcceptToken(),
                savedMember.getId(),
                savedMember.getEmail())
        );
    }

    @Test
    void shouldJoinExistMember() {
        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(memberEntity);
        MemberRoleEntity memberRole = buildMemberRole("Accountant", organization.getId());
        memberRole.setActive(Boolean.FALSE);
        MemberRoleEntity savedMemberRole = memberRoleRepository.save(memberRole);
        memberEntity.setRoles(Set.of(savedMemberRole));
        MemberEntity savedMember = memberRepository.save(memberEntity);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);
        InvitationEntity savedInvitation =
                invitationRepository.save(buildInvitation(savedOrganization.getId(), savedMember.getEmail()));

        OrganizationMembership organizationMembership = organizationService
                .joinOrganization(savedInvitation.getAcceptToken(), savedMember.getId(), savedMember.getEmail());

        assertEquals(savedOrganization.getId(), organizationMembership.getOrg().getId());
        assertEquals(savedMember.getId(), organizationMembership.getMember().getId());
        List<String> actualRoles = organizationMembership.getMember().getRoles().stream()
                .map(MemberRole::getRoleId)
                .collect(Collectors.toList());
        List<String> expectedRoles = savedInvitation.getInviteeRoles().stream()
                .map(MemberRoleEntity::getRoleId)
                .collect(Collectors.toList());
        assertIterableEquals(expectedRoles, actualRoles);
        InvitationEntity invitationEntity = invitationRepository.findById(savedInvitation.getId()).get();
        assertEquals(invitationEntity.getStatus(), InvitationStatusName.ACCEPTED.getValue());
    }

    @Test
    @Transactional
    void shouldJoinNewMember() {
        String userId = TestObjectFactory.randomString();
        String email = TestObjectFactory.randomString();
        OrganizationEntity savedOrg = organizationRepository.save(buildOrganization());
        InvitationEntity savedInvitation = invitationRepository.save(buildInvitation(savedOrg.getId(), email));

        OrganizationMembership organizationMembership = organizationService
                .joinOrganization(savedInvitation.getAcceptToken(), userId, email);

        assertEquals(savedOrg.getId(), organizationMembership.getOrg().getId());
        assertEquals(userId, organizationMembership.getMember().getId());
        List<String> actualRoles = organizationMembership.getMember().getRoles().stream()
                .map(MemberRole::getRoleId)
                .collect(Collectors.toList());
        List<String> expectedRoles = savedInvitation.getInviteeRoles().stream()
                .map(MemberRoleEntity::getRoleId)
                .collect(Collectors.toList());
        assertIterableEquals(expectedRoles, actualRoles);
        InvitationEntity invitationEntity = invitationRepository.findById(savedInvitation.getId()).get();
        assertEquals(invitationEntity.getStatus(), InvitationStatusName.ACCEPTED.getValue());
        assertTrue(memberRoleRepository.findAll().stream().allMatch(MemberRoleEntity::isActive));
    }

    @Test
    @Transactional
    void shouldExpelOrgMember() {
        MemberEntity member = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(member);
        MemberRoleEntity activeRoleInOrg = buildMemberRole("Accountant", organization.getId());
        activeRoleInOrg.setActive(Boolean.TRUE);
        MemberRoleEntity savedMemberRole = memberRoleRepository.save(activeRoleInOrg);
        member.setRoles(Set.of(savedMemberRole));
        MemberEntity savedMember = memberRepository.save(member);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        organizationService.expelOrgMember(savedOrganization.getId(), savedMember.getId());

        OrganizationEntity organizationEntity = organizationRepository.findById(savedOrganization.getId()).get();
        assertTrue(organizationEntity.getMembers().stream().noneMatch(m -> m.getId().equals(savedMember.getId())));
        MemberEntity memberEntity = memberRepository.findById(savedMember.getId()).get();
        assertTrue(memberEntity.getRoles().isEmpty());

        MemberRoleEntity memberRoleEntity = memberRoleRepository.findById(savedMemberRole.getId()).get();
        assertFalse(memberRoleEntity.isActive());
    }

    /**
     * JD-359
     */
    @Test
    @Transactional
    void shouldNotReturnOrganizationsAfterExpelOrgMember() {
        MemberEntity member = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(member);
        MemberRoleEntity activeRoleInOrg = buildMemberRole("Accountant", organization.getId());
        activeRoleInOrg.setActive(Boolean.TRUE);
        MemberRoleEntity savedMemberRole = memberRoleRepository.save(activeRoleInOrg);
        member.setRoles(Set.of(savedMemberRole));
        MemberEntity savedMember = memberRepository.save(member);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        OrganizationSearchResult userOrgs =
                organizationService.findAllOrganizations(savedMember.getId(), null, null);

        assertEquals(1, userOrgs.getResult().size());
        assertEquals(savedOrganization.getId(), userOrgs.getResult().get(0).getId());

        organizationService.expelOrgMember(savedOrganization.getId(), savedMember.getId());

        userOrgs =
                organizationService.findAllOrganizations(savedMember.getId(), null, null);
        assertTrue(userOrgs.getResult().isEmpty());
    }

    @Test
    @Transactional
    void shouldGetListOrgMembers() {
        MemberEntity member1 = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        MemberEntity member2 = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(Set.of(member1, member2));
        MemberRoleEntity activeMember1RoleInOrg = buildMemberRole("Accountant", organization.getId());
        activeMember1RoleInOrg.setActive(Boolean.TRUE);
        MemberRoleEntity savedMember1Role = memberRoleRepository.save(activeMember1RoleInOrg);
        member1.setRoles(Set.of(savedMember1Role));
        MemberRoleEntity activeMember2RoleInOrg = buildMemberRole("Accountant", organization.getId());
        activeMember2RoleInOrg.setActive(Boolean.TRUE);
        MemberRoleEntity savedActiveMember2Role = memberRoleRepository.save(activeMember2RoleInOrg);
        MemberRoleEntity nonActiveMember2RoleInOrg = buildMemberRole("Accountant", organization.getId());
        nonActiveMember2RoleInOrg.setActive(Boolean.FALSE);
        MemberRoleEntity savedNonActiveMember2Role = memberRoleRepository.save(nonActiveMember2RoleInOrg);
        member2.setRoles(Set.of(savedActiveMember2Role, savedNonActiveMember2Role));
        MemberEntity anotherMember = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity anotherOrganization = TestObjectFactory.buildOrganization(Set.of(anotherMember));
        MemberRoleEntity activeAnotherMemberRoleInOrg = buildMemberRole("Accountant", anotherOrganization.getId());
        activeAnotherMemberRoleInOrg.setActive(Boolean.TRUE);
        MemberRoleEntity savedAnotherRole = memberRoleRepository.save(activeAnotherMemberRoleInOrg);
        anotherMember.setRoles(Set.of(savedAnotherRole));
        memberRepository.saveAll(Set.of(member1, member2, anotherMember));
        organizationRepository.saveAll(Set.of(organization, anotherOrganization));

        MemberOrgListResult memberOrgListResult = organizationService.listMembers(organization.getId());

        assertEquals(organization.getMembers().size(), memberOrgListResult.getResult().size());

        List<String> roles = memberOrgListResult.getResult().stream()
                .map(Member::getRoles)
                .flatMap(Collection::stream)
                .map(MemberRole::getId)
                .collect(toList());

        assertThat(roles, hasItem(activeMember1RoleInOrg.getId()));
        assertThat(roles, hasItem(activeMember2RoleInOrg.getId()));
        assertThat(roles, not(hasItem(nonActiveMember2RoleInOrg.getId())));
        assertThat(roles, not(hasItem(activeAnotherMemberRoleInOrg.getId())));
    }

    @Test
    @Transactional
    void shouldThrowLasRoleException() {
        MemberEntity member = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(member);
        MemberRoleEntity role =
                memberRoleRepository.save(TestObjectFactory.buildMemberRole("Accountant", organization.getId()));
        member.setRoles(Set.of(role));
        MemberEntity savedMember = memberRepository.save(member);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        assertThrows(LastRoleException.class,
                () -> organizationService.removeMemberRole(organization.getId(), member.getId(), role.getId()));

    }

    @Test
    @Transactional
    void shouldRemoveMemberRole() {
        MemberEntity memberEntity = TestObjectFactory.testMemberEntity(TestObjectFactory.randomString());
        OrganizationEntity organization = TestObjectFactory.buildOrganization(memberEntity);
        MemberRoleEntity role =
                memberRoleRepository.save(TestObjectFactory.buildMemberRole("Accountant", organization.getId()));
        MemberRoleEntity roleToRemove =
                memberRoleRepository.save(TestObjectFactory.buildMemberRole("Manager", organization.getId()));
        memberEntity.setRoles(Set.of(role, roleToRemove));
        MemberEntity savedMember = memberRepository.save(memberEntity);
        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        organizationService.removeMemberRole(savedOrganization.getId(), savedMember.getId(), roleToRemove.getId());

        assertThat(memberRepository.findById(savedMember.getId()).get().getRoles(), not(hasItem(roleToRemove)));
        assertFalse(memberRoleRepository.findById(roleToRemove.getId()).get().isActive());
    }


}
