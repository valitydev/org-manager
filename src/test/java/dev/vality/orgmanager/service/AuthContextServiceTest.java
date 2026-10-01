package dev.vality.orgmanager.service;

import dev.vality.bouncer.ctx.ContextFragmentType;
import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanagement.PartyNotFound;
import dev.vality.orgmanager.converter.BouncerContextConverter;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.service.model.UserInfo;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthContextServiceTest {

    private final BouncerContextConverter bouncerConverter = new BouncerContextConverter();
    private final TDeserializer byteDeserializer = new TDeserializer();
    @Mock
    private UserService userService;
    @Mock
    private OrganizationRepository organizationRepository;
    private AuthContextService service;

    public AuthContextServiceTest() throws TTransportException {
    }

    @BeforeEach
    void setUp() {
        service = new AuthContextService(userService, bouncerConverter, organizationRepository);
    }

    @Test
    void testUserContext() throws TException {
        var id = TestObjectFactory.randomString();
        var member = TestObjectFactory.testMemberEntity(id);
        var organization = TestObjectFactory.buildOrganization(member);
        var userInfo = new UserInfo(member, Set.of(organization));
        when(userService.findById(id)).thenReturn(userInfo);

        var userContext = service.getUserContext(id);

        verify(userService, times(1)).findById(id);
        var contextFragment = new dev.vality.bouncer.context.v1.ContextFragment();
        byteDeserializer.deserialize(contextFragment, userContext.getContent());

        assertEquals(member.getId(), contextFragment.getUser().getId());
        assertEquals(ContextFragmentType.v1_thrift_binary, userContext.getType());
    }

    @Test
    void testPartyContext() throws TException {
        var organization = TestObjectFactory.buildOrganization();
        organization.setAllowedIps(Set.of("10.0.0.1", "192.168.0.0/24"));
        var partyId = organization.getParty();
        when(organizationRepository.findByParty(partyId)).thenReturn(Optional.of(organization));

        var partyContext = service.getPartyContext(partyId);

        var contextFragment = new dev.vality.bouncer.context.v1.ContextFragment();
        byteDeserializer.deserialize(contextFragment, partyContext.getContent());

        assertEquals(ContextFragmentType.v1_thrift_binary, partyContext.getType());
        assertFalse(contextFragment.isSetUser());
        var party = contextFragment.getParty();
        assertEquals(partyId, party.getId());
        assertEquals(organization.getId(), party.getOrganization().getId());
        assertEquals(organization.getOwner(), party.getOrganization().getOwner().getId());
        assertEquals(organization.getAllowedIps(), party.getOrganization().getAllowedIps());
    }

    @Test
    void testPartyContextWithoutAllowedIps() throws TException {
        var organization = TestObjectFactory.buildOrganization();
        var partyId = organization.getParty();
        when(organizationRepository.findByParty(partyId)).thenReturn(Optional.of(organization));

        var partyContext = service.getPartyContext(partyId);

        var contextFragment = new dev.vality.bouncer.context.v1.ContextFragment();
        byteDeserializer.deserialize(contextFragment, partyContext.getContent());

        assertFalse(contextFragment.getParty().getOrganization().isSetAllowedIps());
    }

    @Test
    void testPartyContextNotFound() {
        var partyId = TestObjectFactory.randomString();
        when(organizationRepository.findByParty(partyId)).thenReturn(Optional.empty());

        assertThrows(PartyNotFound.class, () -> service.getPartyContext(partyId));
    }
}
