package com.rbkmoney.orgmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.orgmanager.OrgManagerApplication;
import com.rbkmoney.orgmanager.entity.InvitationEntity;
import com.rbkmoney.orgmanager.entity.MemberEntity;
import com.rbkmoney.orgmanager.entity.OrganizationEntity;
import com.rbkmoney.orgmanager.exception.AccessDeniedException;
import com.rbkmoney.orgmanager.exception.ResourceNotFoundException;
import com.rbkmoney.orgmanager.repository.InvitationRepository;
import com.rbkmoney.orgmanager.repository.InvitationRepositoryTest;
import com.rbkmoney.orgmanager.repository.MemberRepository;
import com.rbkmoney.orgmanager.repository.OrganizationRepository;
import com.rbkmoney.orgmanager.service.ResourceAccessService;
import com.rbkmoney.swag.organizations.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.rbkmoney.orgmanager.TestObjectFactory.*;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {OrgManagerApplication.class, UserController.class})
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = InvitationRepositoryTest.Initializer.class)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:wiremock.properties")
public class UserControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private KeycloakOpenIdStub keycloakOpenIdStub;

    @SpyBean
    private ResourceAccessService resourceAccessService;

    @Before
    public void setUp() throws Exception {
        keycloakOpenIdStub.givenStub();
        invitationRepository.deleteAll();
        organizationRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    public void joinOrgTestWithResourceNotFound() throws Exception {
        OrganizationJoinRequest organizationJoinRequest = new OrganizationJoinRequest();
        organizationJoinRequest.setInvitation(randomString());
        doThrow(new ResourceNotFoundException()).when(resourceAccessService)
                .checkOrganizationRights(organizationJoinRequest);

        mockMvc.perform(post("/user/membership")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(organizationJoinRequest))
                .header("Authorization", "Bearer " + generateRBKadminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void joinOrgTestWithoutAccess() throws Exception {
        OrganizationJoinRequest organizationJoinRequest = new OrganizationJoinRequest();
        organizationJoinRequest.setInvitation(randomString());
        doThrow(new AccessDeniedException("Access denied")).when(resourceAccessService)
                .checkOrganizationRights(organizationJoinRequest);

        mockMvc.perform(post("/user/membership")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(organizationJoinRequest))
                .header("Authorization", "Bearer " + generateRBKadminJwt())
                .header("X-Request-ID", "testRequestId"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void joinOrgTest() throws Exception {
        String jwtToken = generateRBKadminJwt();
        String userId = getUserFromToken();
        OrganizationEntity savedOrg = organizationRepository.save(buildOrganization());
        InvitationEntity savedInvitation = invitationRepository.save(buildInvitation(savedOrg.getId()));
        OrganizationJoinRequest organizationJoinRequest = new OrganizationJoinRequest();
        organizationJoinRequest.setInvitation(savedInvitation.getAcceptToken());

        MvcResult mvcResult = mockMvc.perform(post("/user/membership")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(organizationJoinRequest))
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Request-ID", "testRequestId")
        ).andExpect(status().isOk()).andReturn();

        OrganizationMembership organizationMembership = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(), OrganizationMembership.class);
        Assert.assertEquals(savedOrg.getId(), organizationMembership.getOrg().getId());
        Assert.assertEquals(userId, organizationMembership.getMember().getId());
        Assert.assertTrue(organizationMembership.getMember().getRoles().stream()
                .anyMatch(memberRole -> memberRole.getRoleId() == RoleId.ADMINISTRATOR));
        Assert.assertTrue(organizationMembership.getMember().getRoles().stream()
                .anyMatch(memberRole -> memberRole.getRoleId() == RoleId.ACCOUNTANT));

        InvitationEntity invitationEntity = invitationRepository.findById(savedInvitation.getId()).get();
        Assert.assertEquals(invitationEntity.getStatus(), InvitationStatusName.ACCEPTED.getValue());
    }

    @Test
    @Transactional
    public void cancelOrgMembershipTest() throws Exception {
        String jwtToken = generateRBKadminJwt();
        String userId = getUserFromToken();
        MemberEntity member = memberRepository.save(testMemberEntity(userId));
        OrganizationEntity orgWithMember = organizationRepository.save(buildOrganization(member));

        mockMvc.perform(delete("/user/membership/{orgId}", orgWithMember.getId())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Request-ID", "testRequestId")
        ).andExpect(status().isOk());

        OrganizationEntity organizationEntity = organizationRepository.findById(orgWithMember.getId()).get();
        Assert.assertFalse(organizationEntity.getMembers().contains(member));
    }

    @Test
    public void inquireOrgMembershipTest() throws Exception {
        String jwtToken = generateRBKadminJwt();
        String userId = getUserFromToken();
        MemberEntity member = memberRepository.save(testMemberEntity(userId));
        OrganizationEntity orgWithMember = organizationRepository.save(buildOrganization(member));

        mockMvc.perform(get("/user/membership/{orgId}", orgWithMember.getId())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Request-ID", "testRequestId")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.org").exists())
                .andExpect(jsonPath("$.org.id", equalTo(orgWithMember.getId())))
                .andExpect(jsonPath("$.member").exists())
                .andExpect(jsonPath("$.member.id", equalTo(userId)));
    }

    @Test
    public void listOrgMembershipWithoutLimitTest() throws Exception {
        String jwtToken = generateRBKadminJwt();
        String userId = getUserFromToken();
        MemberEntity targetMember = memberRepository.save(testMemberEntity(userId));
        Set<OrganizationEntity> targetEntities = buildOrganization(targetMember, 7);
        organizationRepository.saveAll(targetEntities);

        MvcResult mvcResultFirst = mockMvc.perform(get("/user/membership")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Request-ID", "testRequestId")
        ).andExpect(status().isOk()).andReturn();

        OrganizationSearchResult organizationSearchResult = objectMapper.readValue(
                mvcResultFirst.getResponse().getContentAsString(), OrganizationSearchResult.class);
        Assert.assertEquals(7, organizationSearchResult.getResult().size());
    }

    @Test
    public void listOrgMembershipTest() throws Exception {
        String jwtToken = generateRBKadminJwt();
        String userId = getUserFromToken();
        MemberEntity targetMember = memberRepository.save(testMemberEntity(userId));
        Set<OrganizationEntity> targetEntities = buildOrganization(targetMember, 9);
        OrganizationEntity anotherOrganization = buildOrganization();
        OrganizationEntity organizationWithOwner = buildOrganization();
        organizationWithOwner.setOwner(userId);
        targetEntities.addAll(List.of(anotherOrganization, organizationWithOwner));
        organizationRepository.saveAll(targetEntities);
        String limit = "4";

        MvcResult mvcResultFirst = mockMvc.perform(get("/user/membership")
                .queryParam("limit", limit)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Request-ID", "testRequestId")
        ).andExpect(status().isOk()).andReturn();

        OrganizationSearchResult organizationSearchResultFirst = objectMapper.readValue(
                mvcResultFirst.getResponse().getContentAsString(), OrganizationSearchResult.class);
        Assert.assertEquals(4, organizationSearchResultFirst.getResult().size());

        MvcResult mvcResultSecond = mockMvc.perform(get("/user/membership")
                .queryParam("limit", limit)
                .queryParam("continuationToken", organizationSearchResultFirst.getContinuationToken())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Request-ID", "testRequestId")
        ).andExpect(status().isOk()).andReturn();

        OrganizationSearchResult organizationSearchResultSecond = objectMapper.readValue(
                mvcResultSecond.getResponse().getContentAsString(), OrganizationSearchResult.class);
        Assert.assertEquals(4, organizationSearchResultSecond.getResult().size());

        MvcResult mvcResultThird = mockMvc.perform(get("/user/membership")
                .queryParam("limit", limit)
                .queryParam("continuationToken", organizationSearchResultSecond.getContinuationToken())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Request-ID", "testRequestId")
        ).andExpect(status().isOk()).andReturn();

        OrganizationSearchResult organizationSearchResultThird = objectMapper.readValue(
                mvcResultThird.getResponse().getContentAsString(), OrganizationSearchResult.class);

        Assert.assertEquals(2, organizationSearchResultThird.getResult().size());
        Assert.assertNull(organizationSearchResultThird.getContinuationToken());
    }
}