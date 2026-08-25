package dev.vality.orgmanager.service;

import dev.vality.bouncer.decisions.ArbiterSrv;
import dev.vality.bouncer.decisions.Context;
import dev.vality.bouncer.decisions.Judgement;
import dev.vality.bouncer.decisions.Resolution;
import dev.vality.bouncer.decisions.ResolutionAllowed;
import dev.vality.bouncer.decisions.ResolutionRestricted;
import dev.vality.bouncer.decisions.RulesetNotFound;
import dev.vality.orgmanagement.UserNotFound;
import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.config.properties.BouncerProperties;
import dev.vality.orgmanager.exception.BouncerException;
import dev.vality.orgmanager.service.dto.BouncerContextDto;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BouncerServiceImplTest {

    @Mock
    private ArbiterSrv.Iface bouncerClient;

    @Mock
    private BouncerContextFactory bouncerContextFactory;

    private BouncerService bouncerService;


    @BeforeEach
    void setUp() {
        BouncerProperties bouncerProperties = new BouncerProperties();
        bouncerProperties.setRuleSetId(TestObjectFactory.randomString());
        bouncerService = new BouncerServiceImpl(bouncerContextFactory, bouncerClient, bouncerProperties);
    }

    @Test
    void havePrivilegesWithIncorrectBuildContext() throws TException {
        BouncerContextDto bouncerContext = TestObjectFactory.testBouncerContextDto();
        when(bouncerContextFactory.buildContext(bouncerContext)).thenThrow(new UserNotFound());

        var exception = assertThrows(BouncerException.class, () -> bouncerService.havePrivileges(bouncerContext));

        assertThat(exception.getMessage(), containsString("Error while build bouncer context"));
    }

    @Test
    void havePrivilegesWithIncorrectBouncerCall() throws TException {
        BouncerContextDto bouncerContext = TestObjectFactory.testBouncerContextDto();
        when(bouncerContextFactory.buildContext(bouncerContext)).thenReturn(new Context());
        when(bouncerClient.judge(anyString(), any(Context.class))).thenThrow(new RulesetNotFound());

        var exception = assertThrows(BouncerException.class, () -> bouncerService.havePrivileges(bouncerContext));

        assertThat(exception.getMessage(), containsString("Error while call bouncer"));
    }

    @Test
    void havePrivilegesWithRestrictedResolution() throws TException {
        BouncerContextDto bouncerContext = TestObjectFactory.testBouncerContextDto();
        when(bouncerContextFactory.buildContext(bouncerContext)).thenReturn(new Context());
        Judgement judgement = new Judgement();
        Resolution resolution = new Resolution();
        resolution.setRestricted(new ResolutionRestricted());
        judgement.setResolution(resolution);
        when(bouncerClient.judge(anyString(), any(Context.class))).thenReturn(judgement);

        boolean result = bouncerService.havePrivileges(bouncerContext);

        assertFalse(result);
    }

    @Test
    void havePrivilegesWithAllowedResolution() throws TException {
        BouncerContextDto bouncerContext = TestObjectFactory.testBouncerContextDto();
        when(bouncerContextFactory.buildContext(bouncerContext)).thenReturn(new Context());
        Judgement judgement = new Judgement();
        Resolution resolution = new Resolution();
        resolution.setAllowed(new ResolutionAllowed());
        judgement.setResolution(resolution);
        when(bouncerClient.judge(anyString(), any(Context.class))).thenReturn(judgement);

        boolean result = bouncerService.havePrivileges(bouncerContext);

        assertTrue(result);
    }
}
