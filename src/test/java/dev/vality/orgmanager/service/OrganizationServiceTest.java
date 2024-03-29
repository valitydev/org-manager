package dev.vality.orgmanager.service;

import dev.vality.orgmanager.TestObjectFactory;
import dev.vality.orgmanager.converter.MemberConverter;
import dev.vality.orgmanager.converter.OrganizationConverter;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.exception.PartyManagementException;
import dev.vality.orgmanager.exception.ResourceNotFoundException;
import dev.vality.orgmanager.repository.MemberRepository;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.service.dto.MemberWithRoleDto;
import dev.vality.swag.organizations.model.Member;
import dev.vality.swag.organizations.model.MemberOrgListResult;
import dev.vality.swag.organizations.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static dev.vality.orgmanager.TestObjectFactory.testToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationConverter organizationConverter;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private MemberConverter memberConverter;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PartyManagementService partyManagementService;

    @InjectMocks
    private OrganizationService service;

    private static final String OWNER_ID = "testOwnerId";
    private static final String EMAIL = "email@email.org";

    @Test
    void shouldThrowPartyManagementExceptionOnCreate() {
        Organization organization = new Organization();
        OrganizationEntity entity = new OrganizationEntity();
        OrganizationEntity savedEntity = TestObjectFactory.buildOrganization();

        when(organizationConverter.toEntity(organization, OWNER_ID))
                .thenReturn(entity);
        when(organizationRepository.save(entity))
                .thenReturn(savedEntity);
        doThrow(new PartyManagementException())
                .when(partyManagementService).createParty(anyString(), anyString(), anyString());

        assertThrows(PartyManagementException.class,
                () -> service.create(testToken(OWNER_ID, EMAIL), organization, ""));

        verify(organizationConverter, times(1))
                .toEntity(organization, OWNER_ID);
        verify(organizationRepository, times(1))
                .save(entity);
        verify(partyManagementService, times(1))
                .createParty(savedEntity.getParty(), OWNER_ID, EMAIL);
        verify(organizationConverter, times(0))
                .toDomain(any(OrganizationEntity.class));
    }

    @Test
    void shouldCreate() {
        Organization organization = new Organization();
        OrganizationEntity entity = new OrganizationEntity();
        OrganizationEntity savedEntity = TestObjectFactory.buildOrganization();
        Organization savedOrganization = new Organization();
        savedOrganization.setParty(TestObjectFactory.randomString());

        when(organizationConverter.toEntity(organization, OWNER_ID))
                .thenReturn(entity);
        when(organizationRepository.save(entity))
                .thenReturn(savedEntity);
        when(organizationConverter.toDomain(savedEntity))
                .thenReturn(savedOrganization);

        Organization response = service.create(testToken(OWNER_ID, EMAIL), organization, "");

        verify(organizationConverter, times(1))
                .toEntity(organization, OWNER_ID);
        verify(organizationRepository, times(1))
                .save(entity);
        verify(partyManagementService, times(1))
                .createParty(savedEntity.getParty(), OWNER_ID, EMAIL);
        verify(organizationConverter, times(1))
                .toDomain(savedEntity);
        assertThat(response)
                .isEqualTo(savedOrganization);
        assertThat(response.getParty())
                .isEqualTo(savedOrganization.getParty());
    }

    @Test
    void shouldGet() {
        String orgId = "orgId";
        OrganizationEntity entity = new OrganizationEntity();
        Organization organization = new Organization();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(entity));
        when(organizationConverter.toDomain(entity))
                .thenReturn(organization);

        Optional<Organization> response = service.get(orgId);

        assertThat(response.isPresent())
                .isEqualTo(true);
        assertThat(response.get())
                .isEqualTo(organization);
    }

    @Test
    void shouldReturnNotFound() {
        String orgId = "orgId";

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.empty());

        Optional<Organization> response = service.get(orgId);

        assertThat(response.isPresent())
                .isEqualTo(false);
    }

    @Test
    void shouldListMembers() {
        String orgId = TestObjectFactory.randomString();
        Member member = new Member();

        MemberWithRoleDto memberWithRoleDto = getMemberWithRoleDto();
        List<MemberWithRoleDto> memberWithRoleList = List.of(memberWithRoleDto);

        when(organizationRepository.existsById(orgId))
                .thenReturn(true);
        when(memberRepository.getOrgMemberList(orgId))
                .thenReturn(memberWithRoleList);
        when(memberConverter.toDomain(memberWithRoleList))
                .thenReturn(List.of(member));

        MemberOrgListResult response = service.listMembers(orgId);

        assertThat(response)
                .isNotNull();
        assertThat(response.getResult())
                .containsExactly(member);
    }

    private MemberWithRoleDto getMemberWithRoleDto() {
        return new MemberWithRoleDto() {
            @Override
            public String getId() {
                return TestObjectFactory.randomString();
            }

            @Override
            public String getEmail() {
                return TestObjectFactory.randomString();
            }

            @Override
            public String getMemberRoleId() {
                return TestObjectFactory.randomString();
            }

            @Override
            public String getOrganizationId() {
                return TestObjectFactory.randomString();
            }

            @Override
            public String getRoleId() {
                return TestObjectFactory.randomString();
            }

            @Override
            public String getScopeId() {
                return TestObjectFactory.randomString();
            }

            @Override
            public String getResourceId() {
                return TestObjectFactory.randomString();
            }
        };
    }

    @Test
    void shouldReturnNotFoundIfNoOrganizationExistForMembersList() {
        String orgId = "orgId";
        when(organizationRepository.existsById(orgId))
                .thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.listMembers(orgId));
    }

    @Test
    void shouldThrowExceptionIfOrganizationDoesNotExist() {
        String orgId = TestObjectFactory.randomString();
        String userId = TestObjectFactory.randomString();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getOrgMember(userId, orgId));
    }

    @Test
    void shouldThrowExceptionIfUserNotMemberOfOrganization() {
        String orgId = TestObjectFactory.randomString();
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(orgId);
        String userId = TestObjectFactory.randomString();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(organizationEntity));

        assertThrows(ResourceNotFoundException.class, () -> service.getOrgMember(userId, orgId));
    }


    @Test
    void shouldGetOrgMember() {
        String orgId = TestObjectFactory.randomString();
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(orgId);
        String userId = TestObjectFactory.randomString();
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(userId);
        organizationEntity.setMembers(Set.of(memberEntity));
        Member expectedMember = new Member();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(organizationEntity));
        when(memberConverter.toDomain(memberEntity, Collections.emptyList()))
                .thenReturn(expectedMember);

        Member actualMember = service.getOrgMember(userId, orgId);

        assertThat(actualMember)
                .isEqualTo(expectedMember);
    }
}
