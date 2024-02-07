package dev.vality.orgmanager.service;

import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.repository.AbstractRepositoryTest;
import dev.vality.orgmanager.service.model.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceImplTest extends AbstractRepositoryTest {

    @Autowired
    private UserService userService;

    @Test
    void dontFindUserWithoutOrganizations() {
        String userId = TestObjectFactory.randomString();

        UserInfo userInfo = userService.findById(userId);

        assertNull(userInfo.getMember());
        assertEquals(Set.of(), userInfo.getOrganizations());
    }

    @Test
    void findUserWithOwnedOrganizations() {
        String userId = TestObjectFactory.randomString();
        OrganizationEntity organizationEntity = TestObjectFactory.buildOrganization();
        organizationEntity.setOwner(userId);
        organizationRepository.save(organizationEntity);

        UserInfo userInfo = userService.findById(userId);

        assertNull(userInfo.getMember());
        assertEquals(organizationEntity.getId(), userInfo.getOrganizations().iterator().next().getId());
    }

    @Test
    void findMemberUserWithoutOrganizations() {
        String memberId = TestObjectFactory.randomString();
        var member = TestObjectFactory.testMemberEntity(memberId);
        memberRepository.save(member);

        UserInfo userInfo = userService.findById(memberId);
        MemberEntity user = userInfo.getMember();

        assertEquals(member.getId(), user.getId());
        assertEquals(member.getEmail(), user.getEmail());
        assertTrue(userInfo.getOrganizations().isEmpty());
    }

    @Test
    void findMemberUserWithOrganizations() {
        String memberId = TestObjectFactory.randomString();
        var member = TestObjectFactory.testMemberEntity(memberId);
        OrganizationEntity organization = TestObjectFactory.buildOrganization(member);
        MemberRoleEntity memberRole = TestObjectFactory.buildMemberRole("Accountant", organization.getId());
        memberRoleRepository.save(memberRole);
        member.setRoles(Set.of(memberRole));
        memberRepository.save(member);
        organizationRepository.save(organization);

        UserInfo userInfo = userService.findById(memberId);
        MemberEntity user = userInfo.getMember();

        assertEquals(member.getId(), user.getId());
        assertEquals(member.getEmail(), user.getEmail());
        assertEquals(organization.getId(), userInfo.getOrganizations().iterator().next().getId());
    }

    @Test
    void findMemberUserWithMemberAndOwnedOrganizations() {
        String memberId = TestObjectFactory.randomString();
        var member = TestObjectFactory.testMemberEntity(memberId);
        OrganizationEntity organization = TestObjectFactory.buildOrganization(member);
        MemberRoleEntity memberRole = TestObjectFactory.buildMemberRole("Accountant", organization.getId());
        OrganizationEntity ownedOrganization = TestObjectFactory.buildOrganization();
        ownedOrganization.setOwner(memberId);
        memberRoleRepository.save(memberRole);
        member.setRoles(Set.of(memberRole));
        memberRepository.save(member);
        organizationRepository.saveAll(List.of(organization, ownedOrganization));

        UserInfo userInfo = userService.findById(memberId);
        MemberEntity user = userInfo.getMember();

        assertEquals(member.getId(), user.getId());
        assertEquals(member.getEmail(), user.getEmail());
        List<String> actualOrgs = userInfo.getOrganizations()
                .stream()
                .map(OrganizationEntity::getId)
                .collect(Collectors.toList());
        assertTrue(actualOrgs.containsAll(List.of(organization.getId(), ownedOrganization.getId())));
    }

    @Test
    void findMemberUserWithSameMemberAndOwnedOrganizations() {
        String memberId = TestObjectFactory.randomString();
        var member = TestObjectFactory.testMemberEntity(memberId);
        OrganizationEntity organization = TestObjectFactory.buildOrganization(member);
        organization.setOwner(memberId);
        organization.setRoles(
                Set.of(TestObjectFactory.buildOrganizationRole("Accountant", organization.getId()))
        );
        MemberRoleEntity memberRole = TestObjectFactory.buildMemberRole("Accountant", organization.getId());
        memberRoleRepository.save(memberRole);
        member.setRoles(Set.of(memberRole));
        memberRepository.save(member);
        organizationRepository.save(organization);

        UserInfo userInfo = userService.findById(memberId);
        MemberEntity user = userInfo.getMember();

        assertEquals(member.getId(), user.getId());
        assertEquals(member.getEmail(), user.getEmail());
        assertEquals(1, userInfo.getOrganizations().size());
        assertEquals(organization.getId(), userInfo.getOrganizations().iterator().next().getId());
    }
}
