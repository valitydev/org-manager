package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.*;
import dev.vality.orgmanager.config.properties.InviteTokenProperties;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.orgmanager.entity.ScopeEntity;
import dev.vality.orgmanager.repository.InvitationRepository;
import dev.vality.orgmanager.repository.MemberRepository;
import dev.vality.orgmanager.repository.MemberRoleRepository;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.repository.OrganizationRoleRepository;
import dev.vality.orgmanager.repository.ScopeRepository;
import dev.vality.orgmanager.service.dto.MemberWithRoleDto;
import dev.vality.orgmanager.util.JsonCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberRoleRepository memberRoleRepository;
    @Mock
    private OrganizationRoleRepository organizationRoleRepository;
    @Mock
    private ScopeRepository scopeRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private InviteTokenProperties inviteTokenProperties;
    @Mock
    private MailMessageSender mailMessageSender;

    private AdminManagementService service;

    @BeforeEach
    void setUp() {
        AdminManagementConverter converter = new AdminManagementConverter();
        AdminCommonService commonService = new AdminCommonService(
                organizationRepository,
                organizationRoleRepository,
                new JsonCodec(JsonMapper.builder().build()));
        service = new AdminManagementService(
                new AdminOrganizationService(
                        organizationRepository,
                        organizationRoleRepository,
                        scopeRepository,
                        converter,
                        commonService),
                new AdminMemberService(
                        organizationRepository,
                        memberRepository,
                        memberRoleRepository,
                        converter,
                        commonService),
                new AdminInvitationService(
                        invitationRepository,
                        inviteTokenProperties,
                        mailMessageSender,
                        converter,
                        commonService));
    }

    @Test
    void shouldCreateOrganizationWithRequestedPartyAndOwner() throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest("party", "owner", "name")
                .setMetadata("{\"source\":\"admin\"}");
        when(organizationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createOrganization(request);

        assertEquals("party", result.getPartyId());
        assertEquals("owner", result.getOwnerId());
        assertEquals(OrganizationStatus.active, result.getStatus());
        assertEquals(request.getMetadata(), result.getMetadata());
    }

    @Test
    void shouldRejectPartyAlreadyBoundToOrganization() {
        CreateOrganizationRequest request = new CreateOrganizationRequest("party", "owner", "name");
        when(organizationRepository.existsByParty("party")).thenReturn(true);

        assertThrows(PartyAlreadyBound.class, () -> service.createOrganization(request));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void shouldRejectMetadataThatRestWouldFailToRead() {
        CreateOrganizationRequest request = new CreateOrganizationRequest("party", "owner", "name")
                .setMetadata("not a json");

        assertThrows(InvalidRequest.class, () -> service.createOrganization(request));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void shouldRejectBlankOrganizationName() {
        CreateOrganizationRequest request = new CreateOrganizationRequest("party", "owner", "  ");

        assertThrows(InvalidRequest.class, () -> service.createOrganization(request));
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void shouldReturnContinuationTokenWhenMoreOrganizationsExist() {
        ListOrganizationsRequest request = new ListOrganizationsRequest().setLimit(2);
        List<OrganizationEntity> entities = List.of(
                organization("c", OrganizationStatus.active),
                organization("b", OrganizationStatus.active),
                organization("a", OrganizationStatus.active));
        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(entities));

        var result = service.listOrganizations(request);

        assertEquals(List.of("c", "b"), result.getOrganizations().stream().map(it -> it.getId()).toList());
        assertEquals("b", result.getContinuationToken());
    }

    @Test
    void shouldNotReturnContinuationTokenOnLastPage() {
        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(organization("a", OrganizationStatus.active))));

        var result = service.listOrganizations(new ListOrganizationsRequest().setOwnerId("owner-a"));

        assertEquals(List.of("a"), result.getOrganizations().stream().map(it -> it.getId()).toList());
        assertFalse(result.isSetContinuationToken());
    }

    @Test
    void shouldGetOrganizationByParty() throws Exception {
        when(organizationRepository.findByParty("party-org")).thenReturn(Optional.of(
                organization("org", OrganizationStatus.active)));

        assertEquals("org", service.getOrganizationByParty("party-org").getId());
    }

    @Test
    void shouldRejectUnknownParty() {
        when(organizationRepository.findByParty("party-org")).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFound.class, () -> service.getOrganizationByParty("party-org"));
    }

    @Test
    void shouldModifyOnlyRequestedOrganizationFields() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        organization.setMetadata("{\"source\":\"rest\"}");
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var renamed = service.modifyOrganization("org", new ModifyOrganizationRequest().setName("renamed"));

        assertEquals("renamed", renamed.getName());
        assertEquals("{\"source\":\"rest\"}", renamed.getMetadata());

        var remetadated = service.modifyOrganization(
                "org",
                new ModifyOrganizationRequest().setMetadata("{\"source\":\"admin\"}"));

        assertEquals("renamed", remetadated.getName());
        assertEquals("{\"source\":\"admin\"}", remetadated.getMetadata());
    }

    @Test
    void shouldRejectModificationWithBrokenMetadata() {
        when(organizationRepository.findById("org"))
                .thenReturn(Optional.of(organization("org", OrganizationStatus.active)));

        assertThrows(InvalidRequest.class, () -> service.modifyOrganization(
                "org",
                new ModifyOrganizationRequest().setMetadata("not a json")));
    }

    @Test
    void shouldValidateOrganizationStatusTransitions() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var deactivated = service.deactivateOrganization("org");

        assertEquals(OrganizationStatus.deactivated, deactivated.getStatus());
        assertThrows(InvalidOrganizationState.class, () -> service.deactivateOrganization("org"));
        assertEquals(OrganizationStatus.active, service.activateOrganization("org").getStatus());
    }

    @Test
    void shouldRejectUnknownRoleWithRoleNotFound() {
        when(organizationRepository.findById("org"))
                .thenReturn(Optional.of(organization("org", OrganizationStatus.active)));
        when(organizationRoleRepository.findByOrganizationIdAndRoleId("org", "manager"))
                .thenReturn(Optional.empty());

        assertThrows(RoleNotFound.class, () -> service.getOrganizationRole("org", "manager"));
    }

    @Test
    void shouldCreateRoleInCatalogAndThenUpdateIt() throws Exception {
        when(organizationRepository.findById("org"))
                .thenReturn(Optional.of(organization("org", OrganizationStatus.active)));
        when(organizationRoleRepository.findByOrganizationIdAndRoleId("org", "manager"))
                .thenReturn(Optional.empty());
        when(organizationRoleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(scopeRepository.findById("Shop")).thenReturn(Optional.empty());
        when(scopeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.setOrganizationRole(
                "org",
                new SetOrganizationRoleRequest("manager", "Manager", List.of("Shop")));

        assertEquals("manager", created.getId());
        assertEquals("Manager", created.getName());
        assertEquals(List.of("Shop"), created.getScopeIds());

        when(organizationRoleRepository.findByOrganizationIdAndRoleId("org", "manager"))
                .thenReturn(Optional.of(role("manager", "Manager", "Shop")));

        var updated = service.setOrganizationRole(
                "org",
                new SetOrganizationRoleRequest("manager", "Shop manager", new ArrayList<>()));

        assertEquals("Shop manager", updated.getName());
        assertTrue(updated.getScopeIds().isEmpty());
    }

    @Test
    void shouldRejectRoleWithBlankName() {
        when(organizationRepository.findById("org"))
                .thenReturn(Optional.of(organization("org", OrganizationStatus.active)));

        assertThrows(InvalidRequest.class, () -> service.setOrganizationRole(
                "org",
                new SetOrganizationRoleRequest("manager", " ", List.of())));
    }

    @Test
    void shouldAddMemberAndManageRoleWithoutLastRoleRestriction() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRoleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationRoleRepository.findByOrganizationIdAndRoleId("org", "manager"))
                .thenReturn(Optional.of(role("manager", "Manager")));

        var member = service.addMember("org", new AddMemberRequest("user", "user@example.com"));
        var role = service.assignMemberRole("org", "user", new AssignMemberRoleRequest("manager"));
        service.removeMemberRole("org", "user", role.getId());

        assertEquals("user@example.com", member.getEmail());
        assertTrue(service.getMember("org", "user").getRoles().isEmpty());
    }

    @Test
    void shouldUpdateEmailWhenMemberIsAddedTwice() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        MemberEntity existing = MemberEntity.builder()
                .id("user")
                .email("old@example.com")
                .roles(new HashSet<>())
                .build();
        organization.setMembers(new HashSet<>(Set.of(existing)));
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findById("user")).thenReturn(Optional.of(existing));
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var member = service.addMember("org", new AddMemberRequest("user", "new@example.com"));

        assertEquals("new@example.com", member.getEmail());
        assertEquals(1, organization.getMembers().size());
    }

    @Test
    void shouldRejectMemberWithoutEmail() {
        assertThrows(InvalidRequest.class,
                () -> service.addMember("org", new AddMemberRequest().setUserId("user")));
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void shouldRejectRoleThatIsMissingFromCatalog() {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        organization.setMembers(new HashSet<>(Set.of(MemberEntity.builder()
                .id("user")
                .roles(new HashSet<>())
                .build())));
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(organizationRoleRepository.findByOrganizationIdAndRoleId("org", "manager"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidRequest.class,
                () -> service.assignMemberRole("org", "user", new AssignMemberRoleRequest("manager")));
        verifyNoInteractions(memberRoleRepository);
    }

    @Test
    void shouldRejectScopeThatRoleDoesNotAllow() {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        organization.setMembers(new HashSet<>(Set.of(MemberEntity.builder()
                .id("user")
                .roles(new HashSet<>())
                .build())));
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(organizationRoleRepository.findByOrganizationIdAndRoleId("org", "manager"))
                .thenReturn(Optional.of(role("manager", "Manager", "Shop")));

        assertThrows(InvalidRequest.class, () -> service.assignMemberRole(
                "org",
                "user",
                new AssignMemberRoleRequest("manager").setScope(new RoleScope("Wallet"))));
        verifyNoInteractions(memberRoleRepository);
    }

    @Test
    void shouldRejectRemovingRoleFromAnotherOrganization() throws Exception {
        MemberRoleEntity role = MemberRoleEntity.builder()
                .id("role")
                .organizationId("another-org")
                .roleId("manager")
                .active(true)
                .build();
        MemberEntity member = MemberEntity.builder()
                .id("user")
                .roles(new HashSet<>(Set.of(role)))
                .build();
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        organization.setMembers(new HashSet<>(Set.of(member)));
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));

        assertThrows(MemberRoleNotFound.class,
                () -> service.removeMemberRole("org", "user", "role"));
    }

    @Test
    void shouldRevokeOnlyPendingInvitation() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        InvitationEntity expired = invitation("expired", LocalDateTime.now().minusMinutes(1));
        InvitationEntity pending = invitation("pending", LocalDateTime.now().plusDays(1));
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(invitationRepository.findByIdAndOrganizationId("expired", "org")).thenReturn(Optional.of(expired));
        when(invitationRepository.findByIdAndOrganizationId("pending", "org")).thenReturn(Optional.of(pending));

        assertThrows(InvalidInvitationState.class,
                () -> service.revokeInvitation("org", "expired", new RevokeInvitationRequest("obsolete")));
        service.revokeInvitation("org", "pending", new RevokeInvitationRequest("obsolete"));

        assertEquals("Revoked", pending.getStatus());
        assertEquals("obsolete", pending.getRevocationReason());
        verify(invitationRepository).save(pending);
    }

    @Test
    void shouldReturnContinuationTokenWhenMoreInvitationsExist() throws Exception {
        when(organizationRepository.findById("org"))
                .thenReturn(Optional.of(organization("org", OrganizationStatus.active)));
        when(invitationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        invitation("newest", LocalDateTime.now().plusDays(1)),
                        invitation("oldest", LocalDateTime.now().plusDays(1)))));

        var result = service.listInvitations("org", new ListInvitationsRequest().setLimit(1));

        assertEquals(List.of("newest"), result.getInvitations().stream().map(it -> it.getId()).toList());
        assertEquals("newest", result.getContinuationToken());
    }

    @Test
    void shouldReadNextInvitationsPageFromCursorInvitation() throws Exception {
        InvitationEntity cursor = invitation("newest", LocalDateTime.now().plusDays(1));
        when(organizationRepository.findById("org"))
                .thenReturn(Optional.of(organization("org", OrganizationStatus.active)));
        when(invitationRepository.findByIdAndOrganizationId("newest", "org")).thenReturn(Optional.of(cursor));
        when(invitationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(invitation("oldest", LocalDateTime.now().plusDays(1)))));

        var result = service.listInvitations(
                "org",
                new ListInvitationsRequest().setLimit(1).setContinuationToken("newest"));

        assertEquals(List.of("oldest"), result.getInvitations().stream().map(it -> it.getId()).toList());
        assertFalse(result.isSetContinuationToken());
    }

    @Test
    void shouldReturnEmptyPageForUnknownInvitationContinuationToken() throws Exception {
        when(organizationRepository.findById("org"))
                .thenReturn(Optional.of(organization("org", OrganizationStatus.active)));
        when(invitationRepository.findByIdAndOrganizationId("gone", "org")).thenReturn(Optional.empty());

        var result = service.listInvitations(
                "org",
                new ListInvitationsRequest().setContinuationToken("gone"));

        assertTrue(result.getInvitations().isEmpty());
        verify(invitationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldCreateInvitationAndSendMail() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(inviteTokenProperties.getLifeTimeInDays()).thenReturn(7L);
        when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CreateInvitationRequest request = new CreateInvitationRequest("user@example.com", new ArrayList<>());

        var result = service.createInvitation("org", request);

        assertEquals(InvitationStatus.pending, result.getStatus());
        assertEquals("user@example.com", result.getEmail());
        verify(mailMessageSender).send(any(String.class), eq("user@example.com"));
    }

    @Test
    void shouldRejectInvitationWithRoleOutsideCatalog() {
        when(organizationRepository.findById("org"))
                .thenReturn(Optional.of(organization("org", OrganizationStatus.active)));
        when(organizationRoleRepository.findByOrganizationIdAndRoleId("org", "manager"))
                .thenReturn(Optional.empty());
        CreateInvitationRequest request = new CreateInvitationRequest(
                "user@example.com",
                List.of(new RoleAssignment("manager")));

        assertThrows(InvalidRequest.class, () -> service.createInvitation("org", request));
        verifyNoInteractions(mailMessageSender);
    }

    @Test
    void shouldTreatMetadataWrittenByRestAsAbsentWhenItIsJsonNull() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        // REST-слой пишет метаданные через JsonCodec, поэтому при их отсутствии в базе лежит "null"
        organization.setMetadata("null");
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));

        assertFalse(service.getOrganization("org").isSetMetadata());
    }

    @Test
    void shouldListMembersIncludingOnesWithoutRoles() throws Exception {
        when(organizationRepository.existsById("org")).thenReturn(true);
        when(memberRepository.getOrgMemberIds(eq("org"), isNull(), any(Pageable.class)))
                .thenReturn(List.of("user-1", "user-2"));
        when(memberRepository.getOrgMemberListWithRoles(eq("org"), anyCollection())).thenReturn(List.of(
                memberRow("user-1", "user-1@example.com", "role-1", "manager"),
                memberRow("user-1", "user-1@example.com", "role-2", "accountant"),
                memberRow("user-2", "user-2@example.com", null, null)));

        var result = service.listMembers("org", new ListMembersRequest());
        var members = result.getMembers();

        assertEquals(List.of("user-1", "user-2"), members.stream().map(it -> it.getId()).toList());
        assertEquals(List.of("manager", "accountant"),
                members.get(0).getRoles().stream().map(it -> it.getRoleId()).toList());
        assertTrue(members.get(1).getRoles().isEmpty());
        assertEquals("user-2@example.com", members.get(1).getEmail());
        assertFalse(result.isSetContinuationToken());
    }

    @Test
    void shouldReturnContinuationTokenWhenMoreMembersExist() throws Exception {
        when(organizationRepository.existsById("org")).thenReturn(true);
        when(memberRepository.getOrgMemberIds(eq("org"), isNull(), any(Pageable.class)))
                .thenReturn(List.of("user-1", "user-2"));
        when(memberRepository.getOrgMemberListWithRoles(eq("org"), anyCollection())).thenReturn(List.of(
                memberRow("user-1", "user-1@example.com", null, null)));

        var result = service.listMembers("org", new ListMembersRequest().setLimit(1));

        assertEquals(List.of("user-1"), result.getMembers().stream().map(it -> it.getId()).toList());
        assertEquals("user-1", result.getContinuationToken());
    }

    @Test
    void shouldReadNextMembersPageByContinuationToken() throws Exception {
        when(organizationRepository.existsById("org")).thenReturn(true);
        when(memberRepository.getOrgMemberIds(eq("org"), eq("user-1"), any(Pageable.class)))
                .thenReturn(List.of("user-2"));
        when(memberRepository.getOrgMemberListWithRoles(eq("org"), anyCollection())).thenReturn(List.of(
                memberRow("user-2", "user-2@example.com", null, null)));

        var result = service.listMembers(
                "org",
                new ListMembersRequest().setLimit(1).setContinuationToken("user-1"));

        assertEquals(List.of("user-2"), result.getMembers().stream().map(it -> it.getId()).toList());
        assertFalse(result.isSetContinuationToken());
    }

    @Test
    void shouldReturnEmptyPageWhenOrganizationHasNoMembers() throws Exception {
        when(organizationRepository.existsById("org")).thenReturn(true);
        when(memberRepository.getOrgMemberIds(eq("org"), isNull(), any(Pageable.class))).thenReturn(List.of());

        var result = service.listMembers("org", new ListMembersRequest());

        assertTrue(result.getMembers().isEmpty());
        assertNull(result.getContinuationToken());
    }

    @Test
    void shouldRejectListingMembersOfUnknownOrganization() {
        when(organizationRepository.existsById("org")).thenReturn(false);

        assertThrows(OrganizationNotFound.class, () -> service.listMembers("org", new ListMembersRequest()));
        verifyNoInteractions(memberRepository);
    }

    private MemberWithRoleDto memberRow(String memberId, String email, String roleId, String role) {
        return new MemberWithRoleDto() {
            @Override
            public String getId() {
                return memberId;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getMemberRoleId() {
                return roleId;
            }

            @Override
            public String getOrganizationId() {
                return "org";
            }

            @Override
            public String getRoleId() {
                return role;
            }

            @Override
            public String getScopeId() {
                return null;
            }

            @Override
            public String getResourceId() {
                return null;
            }
        };
    }

    private OrganizationRoleEntity role(String roleId, String name, String... scopeIds) {
        Set<ScopeEntity> scopes = new HashSet<>();
        for (String scopeId : scopeIds) {
            scopes.add(ScopeEntity.builder().id(scopeId).build());
        }
        return OrganizationRoleEntity.builder()
                .id("catalog-" + roleId)
                .organizationId("org")
                .roleId(roleId)
                .name(name)
                .possibleScopes(scopes)
                .build();
    }

    private OrganizationEntity organization(String id, OrganizationStatus status) {
        return OrganizationEntity.builder()
                .id(id)
                .party("party-" + id)
                .owner("owner-" + id)
                .name("name-" + id)
                .createdAt(LocalDateTime.now())
                .status(status.name())
                .members(new HashSet<>())
                .roles(new HashSet<>())
                .build();
    }

    private InvitationEntity invitation(String id, LocalDateTime expiresAt) {
        return InvitationEntity.builder()
                .id(id)
                .organizationId("org")
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .inviteeContactEmail("user@example.com")
                .inviteeRoles(new HashSet<>())
                .status("Pending")
                .build();
    }
}
