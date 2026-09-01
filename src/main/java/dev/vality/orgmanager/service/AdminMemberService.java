package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.AddMemberRequest;
import dev.vality.orgmanagement.AssignMemberRoleRequest;
import dev.vality.orgmanagement.Member;
import dev.vality.orgmanagement.MemberNotFound;
import dev.vality.orgmanagement.MemberRole;
import dev.vality.orgmanagement.MemberRoleNotFound;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.repository.MemberRepository;
import dev.vality.orgmanager.repository.MemberRoleRepository;
import dev.vality.orgmanager.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.vality.orgmanager.service.AdminCommonService.collectionOrEmpty;

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
    public List<Member> list(String organizationId) throws OrganizationNotFound {
        log.info("List members: organizationId={}", organizationId);
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFound();
        }
        return converter.toMembers(memberRepository.getOrgMemberListWithRoles(organizationId));
    }

    @Transactional
    public Member add(String organizationId, AddMemberRequest request) throws OrganizationNotFound {
        log.info("Add member: organizationId={}, userId={}", organizationId, request.getUserId());
        // TODO: email в AddMemberRequest опционален, но колонка member.email объявлена NOT NULL
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        MemberEntity member = memberRepository.findById(request.getUserId())
                .orElseGet(() -> MemberEntity.builder()
                        .id(request.getUserId())
                        .roles(new HashSet<>())
                        .organizations(new HashSet<>())
                        .build());
        if (request.isSetEmail()) {
            member.setEmail(request.getEmail());
        }
        member = memberRepository.save(member);
        Set<MemberEntity> members = new HashSet<>(collectionOrEmpty(organization.getMembers()));
        members.add(member);
        organization.setMembers(members);
        organizationRepository.save(organization);
        return converter.toMember(member, organizationId);
    }

    @Transactional
    public void remove(String organizationId, String userId) throws OrganizationNotFound, MemberNotFound {
        log.info("Remove member: organizationId={}, userId={}", organizationId, userId);
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);

        Set<MemberRoleEntity> retainedRoles = new HashSet<>();
        List<MemberRoleEntity> removedRoles = new ArrayList<>();
        for (MemberRoleEntity role : collectionOrEmpty(member.getRoles())) {
            if (organizationId.equals(role.getOrganizationId()) && role.isActive()) {
                role.setActive(false);
                removedRoles.add(role);
            } else {
                retainedRoles.add(role);
            }
        }
        member.setRoles(retainedRoles);
        memberRoleRepository.saveAll(removedRoles);
        memberRepository.save(member);

        Set<MemberEntity> members = new HashSet<>(collectionOrEmpty(organization.getMembers()));
        members.remove(member);
        organization.setMembers(members);
        organizationRepository.save(organization);
    }

    @Transactional
    public MemberRole assignRole(
            String organizationId,
            String userId,
            AssignMemberRoleRequest request) throws OrganizationNotFound, MemberNotFound {
        log.info("Assign member role: organizationId={}, userId={}, roleId={}",
                organizationId, userId, request.getRoleId());
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);
        MemberRoleEntity role = commonService.toMemberRoleEntity(
                organizationId, request.getRoleId(), request.getScope());
        role = memberRoleRepository.save(role);
        Set<MemberRoleEntity> roles = new HashSet<>(collectionOrEmpty(member.getRoles()));
        roles.add(role);
        member.setRoles(roles);
        memberRepository.save(member);
        return converter.toMemberRole(role);
    }

    @Transactional
    public void removeRole(String organizationId, String userId, String memberRoleId)
            throws OrganizationNotFound, MemberNotFound, MemberRoleNotFound {
        log.info("Remove member role: organizationId={}, userId={}, memberRoleId={}",
                organizationId, userId, memberRoleId);
        // TODO: REST-слой запрещает снимать последнюю активную роль участника в организации
        //  (OrganizationService.removeMemberRole -> LastRoleException), административный контракт
        //  это ограничение не повторяет. Нужно либо добавить проверку и исключение в контракт,
        //  либо явно задокументировать расхождение в admin_management.thrift.
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        MemberEntity member = findMember(organization, userId);
        MemberRoleEntity role = collectionOrEmpty(member.getRoles()).stream()
                .filter(candidate -> candidate.getId().equals(memberRoleId))
                .filter(candidate -> organizationId.equals(candidate.getOrganizationId()))
                .filter(MemberRoleEntity::isActive)
                .findFirst()
                .orElseThrow(MemberRoleNotFound::new);
        role.setActive(false);
        Set<MemberRoleEntity> roles = new HashSet<>(collectionOrEmpty(member.getRoles()));
        roles.remove(role);
        member.setRoles(roles);
        memberRoleRepository.save(role);
        memberRepository.save(member);
    }

    private MemberEntity findMember(OrganizationEntity organization, String userId) throws MemberNotFound {
        return collectionOrEmpty(organization.getMembers()).stream()
                .filter(member -> member.getId().equals(userId))
                .findFirst()
                .orElseThrow(MemberNotFound::new);
    }
}
