package dev.vality.orgmanager.service;

import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.config.properties.AccessProperties;
import dev.vality.orgmanager.exception.AccessDeniedException;
import dev.vality.orgmanager.exception.ResourceNotFoundException;
import dev.vality.orgmanager.service.dto.BouncerContextDto;
import dev.vality.orgmanager.service.dto.ResourceDto;
import dev.vality.swag.organizations.model.MemberRole;
import dev.vality.swag.organizations.model.MemberRoleScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ResourceAccessServiceImplTest {

    private AccessProperties accessProperties;
    @Mock
    private BouncerService bouncerService;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private MemberRoleService memberRoleService;

    private ResourceAccessService resourceAccessService;

    @BeforeEach
    void setUp() {
        accessProperties = new AccessProperties();
        accessProperties.setEnabled(true);
        resourceAccessService =
                new ResourceAccessServiceImpl(accessProperties, bouncerService, organizationService, memberRoleService);
    }

    @Test
    void checkRightsNotEnabled() {
        accessProperties.setEnabled(false);

        assertDoesNotThrow(() -> resourceAccessService.checkRights(new ResourceDto()));

        verify(bouncerService, times(0)).havePrivileges(any(BouncerContextDto.class));
    }

    @Test
    void checkRightsWithoutResourceAccess() {
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(false);

        var exception = assertThrows(AccessDeniedException.class, () -> resourceAccessService.checkRights());

        assertThat(exception.getMessage(), containsString("No rights to perform"));
    }

    @Test
    void checkOrgRights() {
        ResourceDto resource = ResourceDto.builder()
                .orgId(TestObjectFactory.randomString())
                .build();
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(true);

        assertDoesNotThrow(() -> resourceAccessService.checkRights(resource));
    }

    @Test
    void checkJoinOrgRightsWithNotExistOrg() {
        ResourceDto resource = ResourceDto.builder()
                .invitationToken(TestObjectFactory.randomString())
                .build();
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(true);
        when(organizationService.getOrgIdByInvitationToken(resource.getInvitationToken()))
                .thenThrow(new ResourceNotFoundException());

        assertThrows(ResourceNotFoundException.class,
                () -> resourceAccessService.checkRights(resource));
    }

    @Test
    void checkJoinOrgRights() {
        ResourceDto resource = ResourceDto.builder()
                .invitationToken(TestObjectFactory.randomString())
                .build();
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(true);

        assertDoesNotThrow(() -> resourceAccessService.checkRights(resource));
    }

    @Test
    void checkMemberRights() {
        ResourceDto resource = ResourceDto.builder()
                .orgId(TestObjectFactory.randomString())
                .memberId(TestObjectFactory.randomString())
                .build();
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(true);

        assertDoesNotThrow(() -> resourceAccessService.checkRights(resource));
    }

    @Test
    void checkRoleRights() {
        ResourceDto resource = ResourceDto.builder()
                .roleId(TestObjectFactory.randomString())
                .scopeResourceId(TestObjectFactory.randomString())
                .build();
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(true);

        assertDoesNotThrow(() -> resourceAccessService.checkRights(resource));
    }

    @Test
    void checkMemberRoleRightsWithNotExistRole() {
        ResourceDto resource = ResourceDto.builder()
                .memberRoleId(TestObjectFactory.randomString())
                .build();
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(true);
        when(memberRoleService.findById(resource.getMemberRoleId()))
                .thenThrow(new ResourceNotFoundException());

        assertThrows(ResourceNotFoundException.class,
                () -> resourceAccessService.checkRights(resource));
    }

    @Test
    void checkMemberRoleRights() {
        ResourceDto resource = ResourceDto.builder()
                .memberRoleId(TestObjectFactory.randomString())
                .build();
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(true);
        when(memberRoleService.findById(resource.getMemberRoleId()))
                .thenReturn(
                        new MemberRole()
                                .roleId("Manager")
                                .scope(new MemberRoleScope().resourceId(TestObjectFactory.randomString()))
                );

        assertDoesNotThrow(() -> resourceAccessService.checkRights(resource));
    }

    @Test
    void checkInvitationRights() {
        ResourceDto resource = ResourceDto.builder()
                .invitationId(TestObjectFactory.randomString())
                .build();
        when(bouncerService.havePrivileges(any(BouncerContextDto.class))).thenReturn(true);

        assertDoesNotThrow(() -> resourceAccessService.checkRights(resource));
    }

}