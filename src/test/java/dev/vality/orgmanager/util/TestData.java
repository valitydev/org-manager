package dev.vality.orgmanager.util;

import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.swag.organizations.model.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestData {

    public static OrganizationEntity buildOrganization(String organizationId, String memberId) {
        MemberEntity member = MemberEntity.builder()
                .id(memberId)
                .email("email")
                .roles(Set.of(MemberRoleEntity.builder()
                        .id("Accountant")
                        .organizationId(organizationId)
                        .roleId("Accountant")
                        .scopeId("Shop")
                        .resourceId("testResourceId")
                        .build()))
                .build();

        return OrganizationEntity.builder()
                .id(organizationId)
                .createdAt(LocalDateTime.now())
                .name("name")
                .owner("owner")
                .party(TestObjectFactory.randomString())
                .members(Set.of(member))
                .build();
    }

    public static InvitationEntity buildInvitation(
            String organizationId,
            String invitationId,
            LocalDateTime expiresAt
    ) {
        return buildInvitation(organizationId, invitationId, expiresAt, InvitationStatusName.PENDING);
    }

    public static InvitationEntity buildInvitation(
            String organizationId,
            String invitationId,
            LocalDateTime expiresAt,
            InvitationStatusName statusName
    ) {
        return InvitationEntity.builder()
                .id(invitationId)
                .acceptToken("token")
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .inviteeContactEmail("contactEmail")
                .inviteeContactType(InviteeContact.TypeEnum.EMAIL.getValue())
                .organizationId(organizationId)
                .status(statusName.getValue())
                .inviteeRoles(Set.of(
                        MemberRoleEntity.builder()
                                .id("role1")
                                .roleId("Administrator")
                                .resourceId("resource1")
                                .scopeId(ResourceScopeId.SHOP.getValue())
                                .organizationId(organizationId)
                                .build(),
                        MemberRoleEntity.builder()
                                .id("role2")
                                .roleId("Manager")
                                .resourceId("resource2")
                                .scopeId(ResourceScopeId.SHOP.getValue())
                                .organizationId(organizationId)
                                .build()))
                .build();
    }

    public static InvitationRequest buildInvitationRequest() {
        InviteeContact inviteeContact = new InviteeContact();
        inviteeContact.setEmail("testEmail@mail.ru");
        inviteeContact.setType(InviteeContact.TypeEnum.EMAIL);
        Invitee invitee = new Invitee();
        invitee.setContact(inviteeContact);
        invitee.setRoles(List.of(buildMemberRole()));
        InvitationRequest invitation = new InvitationRequest();
        invitation.setInvitee(invitee);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("testKey", "testValue");
        invitation.setMetadata(metadata);

        return invitation;
    }

    public static MemberRole buildMemberRole() {
        MemberRole memberRole = new MemberRole();
        memberRole.setRoleId("Administrator");
        MemberRoleScope memberRoleScope = new MemberRoleScope();
        memberRoleScope.setId(ResourceScopeId.SHOP);
        memberRoleScope.setResourceId("testResourceIdKek");
        memberRole.setScope(memberRoleScope);

        return memberRole;
    }

}
