package dev.vality.orgmanager.service;

import dev.vality.bouncer.ctx.ContextFragment;
import dev.vality.bouncer.ctx.ContextFragmentType;
import dev.vality.bouncer.decisions.Context;
import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.config.properties.BouncerProperties;
import dev.vality.orgmanager.converter.BouncerContextConverter;
import dev.vality.orgmanager.service.model.UserInfo;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BouncerContextFactoryTest {

    @Mock
    private UserService userService;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private RequesterIpResolver requesterIpResolver;

    private final BouncerContextConverter bouncerConverter = new BouncerContextConverter();

    private BouncerProperties bouncerProperties;

    private BouncerContextFactory bouncerContextFactory;

    @BeforeEach
    void setUp() {
        bouncerProperties = new BouncerProperties();
        bouncerProperties.setContextFragmentId(TestObjectFactory.randomString());
        bouncerProperties.setAuthMethod(TestObjectFactory.randomString());
        bouncerProperties.setDeploymentId(TestObjectFactory.randomString());
        bouncerProperties.setRealm(TestObjectFactory.randomString());
        bouncerContextFactory =
                new BouncerContextFactory(bouncerConverter, bouncerProperties, userService, keycloakService,
                        requesterIpResolver);
    }

    @Test
    void buildContextSuccess() throws TException {
        var token = TestObjectFactory.testToken();
        var id = token.getSubject();
        var member = TestObjectFactory.testMemberEntity(id);
        var organization = TestObjectFactory.buildOrganization(member);
        var bouncerContext = TestObjectFactory.testBouncerContextDto(id);

        when(userService.findByIdWithActiveOrganizations(id))
                .thenReturn(new UserInfo(member, Set.of(organization)));
        when(keycloakService.getAccessToken()).thenReturn(token);
        when(requesterIpResolver.resolve()).thenReturn(Optional.of("203.0.113.7"));

        Context context = bouncerContextFactory.buildContext(bouncerContext);

        ContextFragment fragment = context.getFragments().get(bouncerProperties.getContextFragmentId());
        var contextFragment = deserialize(fragment);

        assertEquals(ContextFragmentType.v1_thrift_binary, fragment.getType());
        assertEquals(token.getId(), contextFragment.getAuth().getToken().getId());
        assertEquals(member.getId(), contextFragment.getUser().getId());
        assertEquals(bouncerContext.getOperationName(), contextFragment.getOrgmgmt().getOp().getId());
        assertEquals(member.getId(), contextFragment.getOrgmgmt().getOp().getMember().getId());
        assertEquals("203.0.113.7", contextFragment.getRequester().getIp());
        verify(userService, times(2)).findByIdWithActiveOrganizations(anyString());
    }

    @Test
    void buildContextWithoutRequesterIp() throws TException {
        var token = TestObjectFactory.testToken();
        var id = token.getSubject();
        var member = TestObjectFactory.testMemberEntity(id);
        var organization = TestObjectFactory.buildOrganization(member);
        var bouncerContext = TestObjectFactory.testBouncerContextDto(id);

        when(userService.findByIdWithActiveOrganizations(id))
                .thenReturn(new UserInfo(member, Set.of(organization)));
        when(keycloakService.getAccessToken()).thenReturn(token);
        when(requesterIpResolver.resolve()).thenReturn(Optional.empty());

        Context context = bouncerContextFactory.buildContext(bouncerContext);

        var contextFragment = deserialize(context.getFragments().get(bouncerProperties.getContextFragmentId()));
        assertFalse(contextFragment.isSetRequester());
    }

    private static dev.vality.bouncer.context.v1.ContextFragment deserialize(ContextFragment fragment)
            throws TException {
        var contextFragment = new dev.vality.bouncer.context.v1.ContextFragment();
        new TDeserializer().deserialize(contextFragment, fragment.getContent());
        return contextFragment;
    }

}
