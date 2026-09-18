package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.AddMemberRequest;
import dev.vality.orgmanagement.AssignMemberRoleRequest;
import dev.vality.orgmanagement.InvalidRequest;
import dev.vality.orgmanagement.ListMembersRequest;
import dev.vality.orgmanagement.ListMembersResult;
import dev.vality.orgmanagement.Member;
import dev.vality.orgmanagement.MemberNotFound;
import dev.vality.orgmanagement.MemberRole;
import dev.vality.orgmanagement.MemberRoleNotFound;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.RoleScope;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.repository.MemberRepository;
import dev.vality.orgmanager.repository.MemberRoleRepository;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.service.dto.AdminPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.vality.orgmanager.service.AdminCommonService.collectionOrEmpty;
import static dev.vality.orgmanager.service.AdminCommonService.pageLimit;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.function.Function.identity;

/**
 * Участники организаций и их роли в административном контракте.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final AdminManagementConverter converter;
    private final AdminCommonService commonService;

    @Transactional(readOnly = true)
    public Member get(String organizationId, String userId) throws OrganizationNotFound, MemberNotFound {
        log.info("Get member: organizationId={}, userId={}", organizationId, userId);
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        return converter.toMember(findMember(organization, userId), organizationId);
    }

    @Transactional(readOnly = true)
    public ListMembersResult list(String organizationId, ListMembersRequest request) throws OrganizationNotFound {
        log.info("List members: organizationId={}, request={}", organizationId, request);
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFound();
        }
        ListMembersRequest safeRequest = requireNonNullElseGet(request, ListMembersRequest::new);
        int limit = pageLimit(safeRequest.getLimit());
        AdminPage<String> page = AdminPage.of(
                memberRepository.getOrgMemberIds(
                        organizationId,
                        safeRequest.getContinuationToken(),
                        PageRequest.ofSize(limit + 1)),
                limit,
                identity());
        ListMembersResult result = new ListMembersResult(converter.toMembers(
                memberRepository.getOrgMemberListWithRoles(organizationId, page.items())));
        page.continuationToken().ifPresent(result::setContinuationToken);
        return result;
    }

    @Transactional
    public Member add(String organizationId, AddMemberRequest request)
            throws OrganizationNotFound, InvalidRequest {
        log.info("Add member: organizationId={}, userId={}", organizationId, request.getUserId());
        String userId = commonService.requireText(request.getUserId(), "User id");
        String email = commonService.requireText(request.getEmail(), "Email");
        OrganizationEntity organization = commonService.lockOrganization(organizationId);
        memberRepository.upsert(userId, email);
        MemberEntity member = memberRepository.findById(userId).orElseThrow();
        member.setEmail(email);
        Set<MemberEntity> members = organizationMembers(organization);
        if (!containsMember(members, userId)) {
            members.add(member);
        }
        organizationRepository.save(organization);
        return converter.toMember(member, organizationId);
    }

    @Transactional
    public void remove(String organizationId, String userId) throws OrganizationNotFound, MemberNotFound {
        log.info("Remove member: organizationId={}, userId={}", organizationId, userId);
        OrganizationEntity organization = commonService.lockOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);

        List<MemberRoleEntity> removedRoles = memberRoles(member).stream()
                .filter(role -> organizationId.equals(role.getOrganizationId()) && role.isActive())
                .toList();
        removedRoles.forEach(role -> role.setActive(false));
        removedRoles.forEach(memberRoles(member)::remove);
        memberRoleRepository.saveAll(removedRoles);
        memberRepository.save(member);

        organizationMembers(organization).removeIf(candidate -> candidate.getId().equals(userId));
        organizationRepository.save(organization);
    }

    @Transactional
    public MemberRole assignRole(
            String organizationId,
            String userId,
            AssignMemberRoleRequest request) throws OrganizationNotFound, MemberNotFound, InvalidRequest {
        log.info("Assign member role: organizationId={}, userId={}, roleId={}",
                organizationId, userId, request.getRoleId());
        OrganizationEntity organization = commonService.lockOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);
        commonService.validateRoleAssignment(request.getRoleId(), request.getScope());
        MemberRoleEntity assigned = findAssignedRole(member, organizationId, request);
        if (assigned != null) {
            return converter.toMemberRole(assigned);
        }
        MemberRoleEntity role = commonService.toMemberRoleEntity(
                organizationId, request.getRoleId(), request.getScope());
        role = memberRoleRepository.save(role);
        memberRoles(member).add(role);
        memberRepository.save(member);
        return converter.toMemberRole(role);
    }

    @Transactional
    public void removeRole(String organizationId, String userId, String memberRoleId)
            throws OrganizationNotFound, MemberNotFound, MemberRoleNotFound {
        log.info("Remove member role: organizationId={}, userId={}, memberRoleId={}",
                organizationId, userId, memberRoleId);
        OrganizationEntity organization = commonService.lockOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);
        MemberRoleEntity role = collectionOrEmpty(member.getRoles()).stream()
                .filter(candidate -> candidate.getId().equals(memberRoleId))
                .filter(candidate -> organizationId.equals(candidate.getOrganizationId()))
                .filter(MemberRoleEntity::isActive)
                .findFirst()
                .orElseThrow(MemberRoleNotFound::new);
        role.setActive(false);
        memberRoles(member).remove(role);
        memberRoleRepository.save(role);
        memberRepository.save(member);
    }

    /**
     * Уже назначенная роль с той же областью действия, если она есть.
     */
    private MemberRoleEntity findAssignedRole(
            MemberEntity member,
            String organizationId,
            AssignMemberRoleRequest request) {
        RoleScope scope = request.getScope();
        return memberRoles(member).stream()
                .filter(MemberRoleEntity::isActive)
                .filter(role -> organizationId.equals(role.getOrganizationId()))
                .filter(role -> role.getRoleId().equals(request.getRoleId()))
                .filter(role -> scope == null
                        ? role.getScopeId() == null
                        : scope.getScopeId().equals(role.getScopeId())
                        && scope.getResourceId().equals(role.getResourceId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Коллекция меняется на месте: подстановка новой пересоздаёт связующую таблицу целиком.
     */
    private Set<MemberRoleEntity> memberRoles(MemberEntity member) {
        if (member.getRoles() == null) {
            member.setRoles(new HashSet<>());
        }
        return member.getRoles();
    }

    /**
     * Поиск по идентификатору: equals участника учитывает email.
     */
    private boolean containsMember(Set<MemberEntity> members, String userId) {
        return members.stream().anyMatch(member -> member.getId().equals(userId));
    }

    private Set<MemberEntity> organizationMembers(OrganizationEntity organization) {
        if (organization.getMembers() == null) {
            organization.setMembers(new HashSet<>());
        }
        return organization.getMembers();
    }

    private MemberEntity findMember(OrganizationEntity organization, String userId) throws MemberNotFound {
        return collectionOrEmpty(organization.getMembers()).stream()
                .filter(member -> member.getId().equals(userId))
                .findFirst()
                .orElseThrow(MemberNotFound::new);
    }
}
