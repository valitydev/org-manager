package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.AddMemberRequest;
import dev.vality.orgmanagement.AdminManagementSrv;
import dev.vality.orgmanagement.AssignMemberRoleRequest;
import dev.vality.orgmanagement.CreateInvitationRequest;
import dev.vality.orgmanagement.CreateOrganizationRequest;
import dev.vality.orgmanagement.InvalidOrganizationState;
import dev.vality.orgmanagement.Invitation;
import dev.vality.orgmanagement.InvitationNotFound;
import dev.vality.orgmanagement.ListInvitationsRequest;
import dev.vality.orgmanagement.ListOrganizationsRequest;
import dev.vality.orgmanagement.ListOrganizationsResult;
import dev.vality.orgmanagement.Member;
import dev.vality.orgmanagement.MemberNotFound;
import dev.vality.orgmanagement.MemberRole;
import dev.vality.orgmanagement.MemberRoleNotFound;
import dev.vality.orgmanagement.Organization;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.OrganizationRole;
import dev.vality.orgmanagement.PartyAlreadyBound;
import dev.vality.orgmanagement.RevokeInvitationRequest;
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
    public Organization createOrganization(CreateOrganizationRequest request) throws PartyAlreadyBound {
        return adminOrganizationService.create(request);
    }

    @Override
    public Organization getOrganization(String organizationId) throws OrganizationNotFound {
        return adminOrganizationService.get(organizationId);
    }

    @Override
    public ListOrganizationsResult listOrganizations(ListOrganizationsRequest request) {
        return adminOrganizationService.list(request);
    }

    @Override
    public Organization renameOrganization(String organizationId, String name) throws OrganizationNotFound {
        return adminOrganizationService.rename(organizationId, name);
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
    public OrganizationRole getOrganizationRole(String organizationId, String roleId) throws OrganizationNotFound {
        return adminOrganizationService.getRole(organizationId, roleId);
    }

    @Override
    public List<OrganizationRole> listOrganizationRoles(String organizationId) throws OrganizationNotFound {
        return adminOrganizationService.listRoles(organizationId);
    }

    @Override
    public Member getMember(String organizationId, String userId) throws OrganizationNotFound, MemberNotFound {
        return adminMemberService.get(organizationId, userId);
    }

    @Override
    public List<Member> listMembers(String organizationId) throws OrganizationNotFound {
        return adminMemberService.list(organizationId);
    }

    @Override
    public Member addMember(String organizationId, AddMemberRequest request) throws OrganizationNotFound {
        return adminMemberService.add(organizationId, request);
    }

    @Override
    public void removeMember(String organizationId, String userId) throws OrganizationNotFound, MemberNotFound {
        adminMemberService.remove(organizationId, userId);
    }

    @Override
    public MemberRole assignMemberRole(String organizationId, String userId, AssignMemberRoleRequest request)
            throws OrganizationNotFound, MemberNotFound {
        return adminMemberService.assignRole(organizationId, userId, request);
    }

    @Override
    public void removeMemberRole(String organizationId, String userId, String memberRoleId)
            throws OrganizationNotFound, MemberNotFound, MemberRoleNotFound {
        adminMemberService.removeRole(organizationId, userId, memberRoleId);
    }

    @Override
    public Invitation createInvitation(String organizationId, CreateInvitationRequest request)
            throws OrganizationNotFound {
        return adminInvitationService.create(organizationId, request);
    }

    @Override
    public Invitation getInvitation(String organizationId, String invitationId)
            throws OrganizationNotFound, InvitationNotFound {
        return adminInvitationService.get(organizationId, invitationId);
    }

    @Override
    public List<Invitation> listInvitations(String organizationId, ListInvitationsRequest request)
            throws OrganizationNotFound {
        return adminInvitationService.list(organizationId, request);
    }

    @Override
    public void revokeInvitation(String organizationId, String invitationId, RevokeInvitationRequest request)
            throws OrganizationNotFound, InvitationNotFound, InvalidOrganizationState {
        adminInvitationService.revoke(organizationId, invitationId, request);
    }
}
