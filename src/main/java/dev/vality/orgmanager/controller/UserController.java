package dev.vality.orgmanager.controller;

import dev.vality.orgmanager.service.KeycloakService;
import dev.vality.orgmanager.service.OrganizationService;
import dev.vality.orgmanager.service.ResourceAccessService;
import dev.vality.orgmanager.service.dto.ResourceDto;
import dev.vality.swag.organizations.api.UserApi;
import dev.vality.swag.organizations.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final OrganizationService organizationService;
    private final KeycloakService keycloakService;
    private final ResourceAccessService resourceAccessService;

    @Override
    public ResponseEntity<Void> cancelOrgMembership(
            String requestId,
            String orgId) {
        log.info("Cancel org membership: requestId={}, orgId={}", requestId, orgId);
        ResourceDto resource = ResourceDto.builder()
                .orgId(orgId)
                .build();
        resourceAccessService.checkRights(resource);
        AccessToken accessToken = keycloakService.getAccessToken();
        return organizationService.cancelOrgMembership(orgId, accessToken.getSubject(), accessToken.getEmail());
    }

    @Override
    public ResponseEntity<OrganizationMembership> inquireOrgMembership(
            String requestId,
            String orgId) {
        log.info("Inquire org membership: requestId={}, orgId={}", requestId, orgId);
        ResourceDto resource = ResourceDto.builder()
                .orgId(orgId)
                .build();
        resourceAccessService.checkRights(resource);
        AccessToken accessToken = keycloakService.getAccessToken();
        return organizationService.getMembership(orgId, accessToken.getSubject(), accessToken.getEmail());
    }

    @Override
    public ResponseEntity<OrganizationMembership> joinOrg(
            String requestId,
            OrganizationJoinRequest body) {
        log.info("Join organization: requestId={}, body ={}", requestId, body);
        ResourceDto resource = ResourceDto.builder()
                .invitationToken(body.getInvitation())
                .build();
        resourceAccessService.checkRights(resource);
        AccessToken accessToken = keycloakService.getAccessToken();
        return ResponseEntity.ok(organizationService
                .joinOrganization(body.getInvitation(), accessToken.getSubject(), accessToken.getEmail()));
    }

    @Override
    public ResponseEntity<OrganizationSearchResult> listOrgMembership(String requestId,
                                                                      Integer limit,
                                                                      String continuationToken) {
        log.info("List org membership: requestId={}, limit={}, continuationToken={}",
                requestId, limit, continuationToken);
        resourceAccessService.checkRights();
        AccessToken accessToken = keycloakService.getAccessToken();
        OrganizationSearchResult organizationSearchResult =
                organizationService.findAllOrganizations(accessToken.getSubject(), limit, continuationToken);
        return ResponseEntity.ok(organizationSearchResult);
    }

    @Override
    public ResponseEntity<MemberContext> getContext(String requestId) {
        log.info("Get user context. requestId={}", requestId);
        resourceAccessService.checkRights();
        AccessToken accessToken = keycloakService.getAccessToken();
        MemberContext memberContext = organizationService.findMemberContext(accessToken.getSubject());

        return ResponseEntity.ok(memberContext);
    }

    @Override
    public ResponseEntity<Void> switchContext(String requestId,
                                              @Valid OrganizationSwitchRequest organizationSwitchRequest) {
        log.info("Switch user context. requestId={}, body={}", requestId, organizationSwitchRequest);
        ResourceDto resource = ResourceDto.builder()
                .orgId(organizationSwitchRequest.getOrganizationId())
                .build();
        resourceAccessService.checkRights(resource);
        AccessToken accessToken = keycloakService.getAccessToken();

        organizationService.switchMemberContext(
                accessToken.getSubject(),
                organizationSwitchRequest.getOrganizationId()
        );

        return ResponseEntity.noContent().build();
    }
}
