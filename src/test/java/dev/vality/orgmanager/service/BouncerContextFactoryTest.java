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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BouncerContextFactoryTest {

    @Mock
    private UserService userService;

    @Mock
    private KeycloakService keycloakService;

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
                new BouncerContextFactory(bouncerConverter, bouncerProperties, userService, keycloakService);
    }

    @Test
    void buildContextSuccess() throws TException {
        var token = TestObjectFactory.testToken();
        var id = token.getSubject();
        var member = TestObjectFactory.testMemberEntity(id);
        var organization = TestObjectFactory.buildOrganization(member);
        var bouncerContext = TestObjectFactory.testBouncerContextDto(id);

        when(userService.findById(id)).thenReturn(new UserInfo(member, Set.of(organization)));
        when(keycloakService.getAccessToken()).thenReturn(token);

        Context context = bouncerContextFactory.buildContext(bouncerContext);

        ContextFragment fragment = context.getFragments().get(bouncerProperties.getContextFragmentId());
        dev.vality.bouncer.context.v1.ContextFragment contextFragment =
                new dev.vality.bouncer.context.v1.ContextFragment();
        TDeserializer byteDeserializer = new TDeserializer();
        byteDeserializer.deserialize(contextFragment, fragment.getContent());

        assertEquals(ContextFragmentType.v1_thrift_binary, fragment.getType());
        assertEquals(token.getId(), contextFragment.getAuth().getToken().getId());
        assertEquals(member.getId(), contextFragment.getUser().getId());
        assertEquals(bouncerContext.getOperationName(), contextFragment.getOrgmgmt().getOp().getId());
        assertEquals(member.getId(), contextFragment.getOrgmgmt().getOp().getMember().getId());
        verify(userService, times(2)).findById(anyString());
    }

}
