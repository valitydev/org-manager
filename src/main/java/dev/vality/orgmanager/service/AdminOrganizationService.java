package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.CreateOrganizationRequest;
import dev.vality.orgmanagement.InvalidOrganizationState;
import dev.vality.orgmanagement.InvalidRequest;
import dev.vality.orgmanagement.ListOrganizationsRequest;
import dev.vality.orgmanagement.ListOrganizationsResult;
import dev.vality.orgmanagement.ModifyOrganizationRequest;
import dev.vality.orgmanagement.Organization;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.OrganizationRole;
import dev.vality.orgmanagement.PartyAlreadyBound;
import dev.vality.orgmanagement.RoleNotFound;
import dev.vality.orgmanagement.SetOrganizationRoleRequest;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.orgmanager.entity.ScopeEntity;
import dev.vality.orgmanager.entity.StoredOrganizationStatus;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.repository.OrganizationRoleRepository;
import dev.vality.orgmanager.repository.ScopeRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.vality.orgmanager.service.AdminCommonService.collectionOrEmpty;
import static dev.vality.orgmanager.service.AdminCommonService.pageLimit;

/**
 * Организации и их роли в административном контракте.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final ScopeRepository scopeRepository;
    private final AdminManagementConverter converter;
    private final AdminCommonService commonService;

    @Transactional
    public Organization create(CreateOrganizationRequest request) throws PartyAlreadyBound, InvalidRequest {
        log.info("Create organization: partyId={}, ownerId={}", request.getPartyId(), request.getOwnerId());
        commonService.requireText(request.getPartyId(), "Party id");
        commonService.requireText(request.getOwnerId(), "Owner id");
        String name = commonService.requireText(request.getName(), "Organization name");
        String metadata = commonService.toStoredMetadata(request.getMetadata());
        if (organizationRepository.existsByParty(request.getPartyId())) {
            throw new PartyAlreadyBound();
        }
        OrganizationEntity entity = OrganizationEntity.builder()
                .id(UUID.randomUUID().toString())
                .party(request.getPartyId())
                .owner(request.getOwnerId())
                .name(name)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .status(StoredOrganizationStatus.ACTIVE.getValue())
                .members(new HashSet<>())
                .roles(new HashSet<>())
                .build();
        try {
            return converter.toOrganization(organizationRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new PartyAlreadyBound();
        }
    }

    @Transactional(readOnly = true)
    public Organization get(String organizationId) throws OrganizationNotFound {
        log.info("Get organization: organizationId={}", organizationId);
        return converter.toOrganization(commonService.findOrganization(organizationId));
    }

    @Transactional(readOnly = true)
    public Organization getByParty(String partyId) throws OrganizationNotFound {
        log.info("Get organization by party: partyId={}", partyId);
        return converter.toOrganization(organizationRepository.findByParty(partyId)
                .orElseThrow(OrganizationNotFound::new));
    }

    @Transactional(readOnly = true)
    public ListOrganizationsResult list(ListOrganizationsRequest request) {
        log.info("List organizations: request={}", request);
        ListOrganizationsRequest safeRequest = request == null ? new ListOrganizationsRequest() : request;
        int limit = pageLimit(safeRequest.getLimit());
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "id"));
        List<OrganizationEntity> entities = new ArrayList<>(
                organizationRepository.findAll(specification(safeRequest), pageable).getContent());

        String continuationToken = null;
        if (entities.size() > limit) {
            entities = new ArrayList<>(entities.subList(0, limit));
            continuationToken = entities.get(entities.size() - 1).getId();
        }
        ListOrganizationsResult result = new ListOrganizationsResult(
                entities.stream().map(converter::toOrganization).toList());
        if (continuationToken != null) {
            result.setContinuationToken(continuationToken);
        }
        return result;
    }

    @Transactional
    public Organization modify(String organizationId, ModifyOrganizationRequest request)
            throws OrganizationNotFound, InvalidRequest {
        log.info("Modify organization: organizationId={}, request={}", organizationId, request);
        ModifyOrganizationRequest safeRequest = request == null ? new ModifyOrganizationRequest() : request;
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        if (safeRequest.isSetName()) {
            organization.setName(commonService.requireText(safeRequest.getName(), "Organization name"));
        }
        if (safeRequest.isSetMetadata()) {
            organization.setMetadata(commonService.toStoredMetadata(safeRequest.getMetadata()));
        }
        return converter.toOrganization(organizationRepository.save(organization));
    }

    @Transactional
    public Organization deactivate(String organizationId) throws OrganizationNotFound, InvalidOrganizationState {
        log.info("Deactivate organization: organizationId={}", organizationId);
        return changeStatus(organizationId, StoredOrganizationStatus.ACTIVE, StoredOrganizationStatus.DEACTIVATED);
    }

    @Transactional
    public Organization activate(String organizationId) throws OrganizationNotFound, InvalidOrganizationState {
        log.info("Activate organization: organizationId={}", organizationId);
        return changeStatus(organizationId, StoredOrganizationStatus.DEACTIVATED, StoredOrganizationStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public OrganizationRole getRole(String organizationId, String roleId)
            throws OrganizationNotFound, RoleNotFound {
        log.info("Get organization role: organizationId={}, roleId={}", organizationId, roleId);
        commonService.findOrganization(organizationId);
        OrganizationRoleEntity role = organizationRoleRepository.findByOrganizationIdAndRoleId(organizationId, roleId)
                .orElseThrow(RoleNotFound::new);
        return converter.toOrganizationRole(role);
    }

    @Transactional(readOnly = true)
    public List<OrganizationRole> listRoles(String organizationId) throws OrganizationNotFound {
        log.info("List organization roles: organizationId={}", organizationId);
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        return collectionOrEmpty(organization.getRoles()).stream()
                .sorted(Comparator.comparing(OrganizationRoleEntity::getRoleId))
                .map(converter::toOrganizationRole)
                .toList();
    }

    /**
     * Создаёт либо обновляет роль в каталоге ролей организации. Каталог задаёт, какие роли
     * и области действия допустимы в AssignMemberRole и CreateInvitation
     */
    @Transactional
    public OrganizationRole setRole(String organizationId, SetOrganizationRoleRequest request)
            throws OrganizationNotFound, InvalidRequest {
        log.info("Set organization role: organizationId={}, request={}", organizationId, request);
        if (request == null) {
            throw new InvalidRequest("Request must not be null");
        }
        commonService.findOrganization(organizationId);
        String roleId = commonService.requireText(request.getRoleId(), "Role id");
        String name = commonService.requireText(request.getName(), "Role name");
        OrganizationRoleEntity role = organizationRoleRepository
                .findByOrganizationIdAndRoleId(organizationId, roleId)
                .orElseGet(() -> OrganizationRoleEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .organizationId(organizationId)
                        .roleId(roleId)
                        .build());
        role.setName(name);
        role.setPossibleScopes(resolveScopes(request.getScopeIds()));
        return converter.toOrganizationRole(organizationRoleRepository.save(role));
    }

    private Set<ScopeEntity> resolveScopes(List<String> scopeIds) throws InvalidRequest {
        Set<ScopeEntity> scopes = new LinkedHashSet<>();
        for (String scopeId : scopeIds == null ? List.<String>of() : scopeIds) {
            commonService.requireText(scopeId, "Scope id");
            scopes.add(scopeRepository.findById(scopeId)
                    .orElseGet(() -> scopeRepository.save(ScopeEntity.builder().id(scopeId).build())));
        }
        return scopes;
    }

    private Specification<OrganizationEntity> specification(ListOrganizationsRequest request) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.isSetStatus()) {
                predicates.add(builder.equal(root.get("status"), request.getStatus().name()));
            }
            if (request.isSetOwnerId()) {
                predicates.add(builder.equal(root.get("owner"), request.getOwnerId()));
            }
            if (request.isSetContinuationToken()) {
                predicates.add(builder.lessThan(root.get("id"), request.getContinuationToken()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Organization changeStatus(
            String organizationId,
            StoredOrganizationStatus expected,
            StoredOrganizationStatus target) throws OrganizationNotFound, InvalidOrganizationState {
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        String current = organization.getStatus();
        if (!expected.matches(current)) {
            throw new InvalidOrganizationState(
                    "Expected organization status " + expected.getValue() + ", but was " + current);
        }
        organization.setStatus(target.getValue());
        return converter.toOrganization(organizationRepository.save(organization));
    }
}
