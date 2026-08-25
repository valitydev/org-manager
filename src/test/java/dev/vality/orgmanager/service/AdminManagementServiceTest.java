package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.AddMemberRequest;
import dev.vality.orgmanagement.AssignMemberRoleRequest;
import dev.vality.orgmanagement.CreateInvitationRequest;
import dev.vality.orgmanagement.CreateOrganizationRequest;
import dev.vality.orgmanagement.InvalidOrganizationState;
import dev.vality.orgmanagement.InvitationStatus;
import dev.vality.orgmanagement.ListInvitationsRequest;
import dev.vality.orgmanagement.ListOrganizationsRequest;
import dev.vality.orgmanagement.MemberRoleNotFound;
import dev.vality.orgmanagement.OrganizationStatus;
import dev.vality.orgmanagement.PartyAlreadyBound;
import dev.vality.orgmanagement.RevokeInvitationRequest;
import dev.vality.orgmanager.config.properties.InviteTokenProperties;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.InvitationEntity;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.repository.InvitationRepository;
import dev.vality.orgmanager.repository.MemberRepository;
import dev.vality.orgmanager.repository.MemberRoleRepository;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.repository.OrganizationRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private InvitationRepository invitationRepository;
    @Mock
    private InviteTokenProperties inviteTokenProperties;
    @Mock
    private MailMessageSender mailMessageSender;

    private AdminManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminManagementService(
                organizationRepository,
                memberRepository,
                memberRoleRepository,
                organizationRoleRepository,
                invitationRepository,
                new AdminManagementConverter(),
                inviteTokenProperties,
                mailMessageSender);
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
    void shouldReturnContinuationTokenWhenMoreOrganizationsExist() {
        ListOrganizationsRequest request = new ListOrganizationsRequest().setLimit(2);
        List<OrganizationEntity> entities = List.of(
                organization("c", OrganizationStatus.active),
                organization("b", OrganizationStatus.active),
                organization("a", OrganizationStatus.active));
        when(organizationRepository.findAllByOrderByIdDesc(any(Pageable.class))).thenReturn(entities);

        var result = service.listOrganizations(request);

        assertEquals(List.of("c", "b"), result.getOrganizations().stream().map(it -> it.getId()).toList());
        assertEquals("b", result.getContinuationToken());
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
    void shouldAddMemberAndManageRoleWithoutLastRoleRestriction() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRoleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var member = service.addMember("org", new AddMemberRequest("user").setEmail("user@example.com"));
        var role = service.assignMemberRole("org", "user", new AssignMemberRoleRequest("manager"));
        service.removeMemberRole("org", "user", role.getId());

        assertEquals("user@example.com", member.getEmail());
        assertTrue(service.getMember("org", "user").getRoles().isEmpty());
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
    void shouldUseEffectiveInvitationStatusForListingAndRevocation() throws Exception {
        OrganizationEntity organization = organization("org", OrganizationStatus.active);
        InvitationEntity expired = invitation("expired", LocalDateTime.now().minusMinutes(1));
        InvitationEntity pending = invitation("pending", LocalDateTime.now().plusDays(1));
        when(organizationRepository.findById("org")).thenReturn(Optional.of(organization));
        when(invitationRepository.findByOrganizationId("org")).thenReturn(List.of(expired, pending));
        when(invitationRepository.findByIdAndOrganizationId("expired", "org")).thenReturn(Optional.of(expired));
        when(invitationRepository.findByIdAndOrganizationId("pending", "org")).thenReturn(Optional.of(pending));

        var result = service.listInvitations(
                "org",
                new ListInvitationsRequest().setStatus(InvitationStatus.expired));

        assertEquals(List.of("expired"), result.stream().map(it -> it.getId()).toList());
        assertThrows(InvalidOrganizationState.class,
                () -> service.revokeInvitation("org", "expired", new RevokeInvitationRequest("obsolete")));
        service.revokeInvitation("org", "pending", new RevokeInvitationRequest("obsolete"));
        assertEquals("Revoked", pending.getStatus());
        assertEquals("obsolete", pending.getRevocationReason());
        verify(invitationRepository).save(pending);
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
