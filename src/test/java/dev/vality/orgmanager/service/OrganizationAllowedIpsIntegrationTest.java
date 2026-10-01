package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.CreateOrganizationRequest;
import dev.vality.orgmanagement.InvalidRequest;
import dev.vality.orgmanagement.ModifyOrganizationRequest;
import dev.vality.orgmanagement.Organization;
import dev.vality.orgmanager.repository.AbstractRepositoryTest;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationAllowedIpsIntegrationTest extends AbstractRepositoryTest {

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private AuthContextService authContextService;

    private final String suffix = UUID.randomUUID().toString();

    @Test
    void shouldCreateOrganizationWithAllowedIps() throws Exception {
        Organization created = create(Set.of(" 1.2.3.4", "10.0.0.1", "2001:db8::1"));

        Set<String> expected = Set.of("1.2.3.4", "10.0.0.1", "2001:db8::1");
        assertEquals(expected, created.getAllowedIps());
        assertEquals(expected, adminManagementService.getOrganization(created.getId()).getAllowedIps());
        assertEquals(expected, organizationRepository.findById(created.getId()).orElseThrow().getAllowedIps());
    }

    @Test
    void shouldCreateOrganizationWithoutAllowedIps() throws Exception {
        Organization created = create(null);

        assertFalse(created.isSetAllowedIps());
        assertFalse(adminManagementService.getOrganization(created.getId()).isSetAllowedIps());
    }

    @Test
    void shouldReplaceAllowedIpsOnModify() throws Exception {
        String organizationId = create(Set.of("1.2.3.4", "5.6.7.8")).getId();

        Organization modified = adminManagementService.modifyOrganization(
                organizationId, new ModifyOrganizationRequest().setAllowedIps(Set.of("5.6.7.8", "192.168.0.1")));

        assertEquals(Set.of("5.6.7.8", "192.168.0.1"), modified.getAllowedIps());
        assertEquals(Set.of("5.6.7.8", "192.168.0.1"),
                adminManagementService.getOrganization(organizationId).getAllowedIps());
    }

    @Test
    void shouldKeepAllowedIpsWhenNotSetInModify() throws Exception {
        String organizationId = create(Set.of("1.2.3.4")).getId();

        Organization modified = adminManagementService.modifyOrganization(
                organizationId, new ModifyOrganizationRequest().setName("Renamed"));

        assertEquals("Renamed", modified.getName());
        assertEquals(Set.of("1.2.3.4"), adminManagementService.getOrganization(organizationId).getAllowedIps());
    }

    @Test
    void shouldClearAllowedIpsWithEmptySet() throws Exception {
        String organizationId = create(Set.of("1.2.3.4")).getId();

        adminManagementService.modifyOrganization(
                organizationId, new ModifyOrganizationRequest().setAllowedIps(Set.of()));

        assertFalse(adminManagementService.getOrganization(organizationId).isSetAllowedIps());
    }

    @Test
    void shouldRejectInvalidAllowedIps() throws Exception {
        assertThrows(InvalidRequest.class, () -> create(Set.of("1.2.3.4", "not-an-ip")));

        String organizationId = create(Set.of("1.2.3.4")).getId();
        assertThrows(InvalidRequest.class, () -> adminManagementService.modifyOrganization(
                organizationId, new ModifyOrganizationRequest().setAllowedIps(Set.of("10.0.0.1/8"))));
        assertEquals(Set.of("1.2.3.4"), adminManagementService.getOrganization(organizationId).getAllowedIps());
    }

    @Test
    void shouldPassAllowedIpsToUserContext() throws Exception {
        String organizationId = create(Set.of("1.2.3.4", "10.0.0.1")).getId();

        var organization = userContext(owner()).getUser().getOrgs().stream()
                .filter(org -> org.getId().equals(organizationId))
                .findFirst()
                .orElseThrow();
        assertEquals(Set.of("1.2.3.4", "10.0.0.1"), organization.getAllowedIps());

        adminManagementService.modifyOrganization(
                organizationId, new ModifyOrganizationRequest().setAllowedIps(Set.of()));

        var cleared = userContext(owner()).getUser().getOrgs().stream()
                .filter(org -> org.getId().equals(organizationId))
                .findFirst()
                .orElseThrow();
        assertFalse(cleared.isSetAllowedIps());
    }

    private Organization create(Set<String> allowedIps) throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "party-" + UUID.randomUUID(), owner(), "Organization");
        if (allowedIps != null) {
            request.setAllowedIps(allowedIps);
        }
        return adminManagementService.createOrganization(request);
    }

    private String owner() {
        return "owner-" + suffix;
    }

    private dev.vality.bouncer.context.v1.ContextFragment userContext(String userId) throws Exception {
        var fragment = new dev.vality.bouncer.context.v1.ContextFragment();
        new TDeserializer(new TBinaryProtocol.Factory())
                .deserialize(fragment, authContextService.getUserContext(userId).getContent());
        return fragment;
    }
}
