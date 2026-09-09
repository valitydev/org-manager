package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminManagementService implements AdminManagementSrv.Iface {

    private final AdminOrganizationService adminOrganizationService;
    private final AdminMemberService adminMemberService;
    private final AdminInvitationService adminInvitationService;

    @Override
    public Organization createOrganization(CreateOrganizationRequest request)
            throws PartyAlreadyBound, InvalidRequest {
        return adminOrganizationService.create(request);
    }

    @Override
    public Organization getOrganization(String organizationId) throws OrganizationNotFound {
        return adminOrganizationService.get(organizationId);
    }

    @Override
    public Organization getOrganizationByParty(String partyId) throws OrganizationNotFound {
        return adminOrganizationService.getByParty(partyId);
    }

    @Override
    public ListOrganizationsResult listOrganizations(ListOrganizationsRequest request) {
        return adminOrganizationService.list(request);
    }

    @Override
    public Organization modifyOrganization(String organizationId, ModifyOrganizationRequest request)
            throws OrganizationNotFound, InvalidRequest {
        return adminOrganizationService.modify(organizationId, request);
    }

    @Override
    public Organization deactivateOrganization(String organizationId)
            throws OrganizationNotFound, InvalidOrganizationState {
        return adminOrganizationService.deactivate(organizationId);
    }

    @Override
    public Organization activateOrganization(String organizationId)
            throws OrganizationNotFound, InvalidOrganizationState {
        return adminOrganizationService.activate(organizationId);
    }

    @Override
    public OrganizationRole getOrganizationRole(String organizationId, String roleId)
            throws OrganizationNotFound, RoleNotFound {
        return adminOrganizationService.getRole(organizationId, roleId);
    }

    @Override
    public List<OrganizationRole> listOrganizationRoles(String organizationId) throws OrganizationNotFound {
        return adminOrganizationService.listRoles(organizationId);
    }

    @Override
    public OrganizationRole setOrganizationRole(String organizationId, SetOrganizationRoleRequest request)
            throws OrganizationNotFound, InvalidRequest {
        return adminOrganizationService.setRole(organizationId, request);
    }

    @Override
    public Member getMember(String organizationId, String userId) throws OrganizationNotFound, MemberNotFound {
        return adminMemberService.get(organizationId, userId);
    }

    @Override
    public ListMembersResult listMembers(String organizationId, ListMembersRequest request)
            throws OrganizationNotFound {
        return adminMemberService.list(organizationId, request);
    }

    @Override
    public Member addMember(String organizationId, AddMemberRequest request)
            throws OrganizationNotFound, InvalidRequest {
        return adminMemberService.add(organizationId, request);
    }

    @Override
    public void removeMember(String organizationId, String userId) throws OrganizationNotFound, MemberNotFound {
        adminMemberService.remove(organizationId, userId);
    }

    @Override
    public MemberRole assignMemberRole(String organizationId, String userId, AssignMemberRoleRequest request)
            throws OrganizationNotFound, MemberNotFound, InvalidRequest {
        return adminMemberService.assignRole(organizationId, userId, request);
    }

    @Override
    public void removeMemberRole(String organizationId, String userId, String memberRoleId)
            throws OrganizationNotFound, MemberNotFound, MemberRoleNotFound {
        adminMemberService.removeRole(organizationId, userId, memberRoleId);
    }

    @Override
    public Invitation createInvitation(String organizationId, CreateInvitationRequest request)
            throws OrganizationNotFound, InvalidRequest {
        return adminInvitationService.create(organizationId, request);
    }

    @Override
    public Invitation getInvitation(String organizationId, String invitationId)
            throws OrganizationNotFound, InvitationNotFound {
        return adminInvitationService.get(organizationId, invitationId);
    }

    @Override
    public ListInvitationsResult listInvitations(String organizationId, ListInvitationsRequest request)
            throws OrganizationNotFound {
        return adminInvitationService.list(organizationId, request);
    }

    @Override
    public void revokeInvitation(String organizationId, String invitationId, RevokeInvitationRequest request)
            throws OrganizationNotFound, InvitationNotFound, InvalidInvitationState {
        adminInvitationService.revoke(organizationId, invitationId, request);
    }
}
