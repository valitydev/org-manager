package dev.vality.orgmanager;

import dev.vality.bouncer.base.Entity;
import dev.vality.bouncer.context.v1.OrgRole;
import dev.vality.bouncer.context.v1.OrgRoleScope;
import dev.vality.bouncer.context.v1.Organization;
import dev.vality.bouncer.context.v1.User;
import dev.vality.orgmanager.entity.*;
import dev.vality.orgmanager.service.dto.BouncerContextDto;
import dev.vality.orgmanager.service.dto.RoleDto;
import dev.vality.swag.organizations.model.*;
import org.keycloak.representations.AccessToken;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class TestObjectFactory {

    public static User testUser() {
        return new User()
                .setId(randomString())
                .setEmail(randomString())
                .setOrgs(Set.of(testOrganization()));
    }

    public static Organization testOrganization() {
        return new Organization()
                .setId(randomString())
                .setOwner(new Entity().setId(randomString()))
                .setRoles(Set.of(testOrgRole()));
    }

    public static OrgRole testOrgRole() {
        return new OrgRole()
                .setId(randomString())
                .setScope(new OrgRoleScope()
                        .setShop(new Entity().setId(randomString()))
                );
    }

    public static AccessToken testToken() {
        return testToken(randomString(), randomString());
    }

    public static AccessToken testToken(String subject, String email) {
        AccessToken token = new AccessToken();
        token.exp(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .subject(subject)
                .id(randomString());
        token.setEmail(email);
        return token;
    }

    public static MemberEntity testMemberEntity(String id) {
        return MemberEntity.builder()
                .email(randomString())
                .id(id)
                .build();
    }

    public static BouncerContextDto testBouncerContextDto() {
        return BouncerContextDto.builder()
                .memberId(randomString())
                .operationName(randomString())
                .organizationId(randomString())
                .role(testRoleDto())
                .build();
    }

    public static BouncerContextDto testBouncerContextDto(String id) {
        return BouncerContextDto.builder()
                .memberId(id)
                .operationName(randomString())
                .organizationId(randomString())
                .role(testRoleDto())
                .build();
    }

    public static RoleDto testRoleDto() {
        return RoleDto.builder()
                .roleId(randomString())
                .scopeResourceId(randomString())
                .build();
    }

    public static MemberRole testMemberRole() {
        MemberRole memberRole = new MemberRole();
        memberRole.setRoleId("Manager");
        return memberRole;
    }

    public static OrganizationJoinRequest testOrganizationJoinRequest() {
        OrganizationJoinRequest organizationJoinRequest = new OrganizationJoinRequest();
        organizationJoinRequest.setInvitation(randomString());
        return organizationJoinRequest;
    }

    public static InvitationRequest testInvitationRequest() {
        Invitee invitee = new Invitee();
        InviteeContact inviteeContact = new InviteeContact();
        inviteeContact.setEmail(randomString());
        inviteeContact.setType(InviteeContact.TypeEnum.EMAIL);
        invitee.setContact(inviteeContact);
        InvitationRequest invitationRequest = new InvitationRequest();
        invitationRequest.setInvitee(invitee);
        return invitationRequest;
    }

    public static OrganizationEntity buildOrganization() {
        OrganizationEntity entity = new OrganizationEntity();
        entity.setId(randomString());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setName(randomString());
        entity.setOwner(randomString());
        entity.setParty(randomString());
        entity.setMetadata("{\"a\":\"b\"}");
        return entity;
    }

    public static OrganizationEntity buildOrganization(MemberEntity memberEntity) {
        return OrganizationEntity.builder()
                .id(randomString())
                .createdAt(LocalDateTime.now())
                .name(randomString())
                .owner(randomString())
                .party(randomString())
                .members(Set.of(memberEntity))
                .build();
    }

    public static OrganizationEntity buildOrganization(Set<MemberEntity> entities) {
        return OrganizationEntity.builder()
                .id(randomString())
                .createdAt(LocalDateTime.now())
                .name(randomString())
                .owner(randomString())
                .party(randomString())
                .members(entities)
                .build();
    }

    public static Set<OrganizationEntity> buildOrganization(MemberEntity memberEntity, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> buildOrganization(memberEntity))
                .collect(Collectors.toSet());
    }

    public static InvitationEntity buildInvitation(String orgId) {
        return buildInvitation(orgId, "contactEmail");
    }

    public static InvitationEntity buildInvitation(String orgId, String email) {
        return InvitationEntity.builder()
                .id(randomString())
                .acceptToken(randomString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .inviteeContactEmail(email)
                .inviteeContactType("contactType")
                .metadata("metadata")
                .organizationId(orgId)
                .status("Pending")
                .inviteeRoles(Set.of(
                        buildMemberRole("Administrator", orgId),
                        buildMemberRole("Accountant", orgId)))
                .build();
    }

    public static MemberRoleEntity buildMemberRole(String role, String orgId) {
        return MemberRoleEntity.builder()
                .id(randomString())
                .roleId(role)
                .resourceId(randomString())
                .scopeId(ResourceScopeId.SHOP.getValue())
                .organizationId(orgId)
                .active(true)
                .build();
    }

    public static OrganizationRoleEntity buildOrganizationRole(String role, String orgId) {
        return OrganizationRoleEntity.builder()
                .id(randomString())
                .roleId(role)
                .organizationId(orgId)
                .name(randomString())
                .build();
    }

    public static String randomString() {
        return UUID.randomUUID().toString();
    }

}
