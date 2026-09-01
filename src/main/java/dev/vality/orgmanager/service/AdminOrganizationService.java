package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.CreateOrganizationRequest;
import dev.vality.orgmanagement.InvalidOrganizationState;
import dev.vality.orgmanagement.ListOrganizationsRequest;
import dev.vality.orgmanagement.ListOrganizationsResult;
import dev.vality.orgmanagement.Organization;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.OrganizationRole;
import dev.vality.orgmanagement.OrganizationStatus;
import dev.vality.orgmanagement.PartyAlreadyBound;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.OrganizationRoleEntity;
import dev.vality.orgmanager.entity.StoredOrganizationStatus;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.repository.OrganizationRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static dev.vality.orgmanager.service.AdminCommonService.collectionOrEmpty;

/**
 * Организации и их роли в административном контракте.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrganizationService {

    static final int DEFAULT_ORGANIZATION_LIMIT = 20;
    static final int MAX_ORGANIZATION_LIMIT = 1000;

    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final AdminManagementConverter converter;
    private final AdminCommonService commonService;

    @Transactional
    public Organization create(CreateOrganizationRequest request) throws PartyAlreadyBound {
        log.info("Create organization: partyId={}, ownerId={}", request.getPartyId(), request.getOwnerId());
        if (organizationRepository.existsByParty(request.getPartyId())) {
            throw new PartyAlreadyBound();
        }
        OrganizationEntity entity = OrganizationEntity.builder()
                .id(UUID.randomUUID().toString())
                .party(request.getPartyId())
                .owner(request.getOwnerId())
                .name(request.getName())
                .metadata(commonService.toStoredMetadata(request.getMetadata()))
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
    public ListOrganizationsResult list(ListOrganizationsRequest request) {
        log.info("List organizations: request={}", request);
        ListOrganizationsRequest safeRequest = request == null ? new ListOrganizationsRequest() : request;
        int limit = organizationLimit(safeRequest);
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<OrganizationEntity> entities = findOrganizations(safeRequest, pageable);

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
    public Organization rename(String organizationId, String name) throws OrganizationNotFound {
        log.info("Rename organization: organizationId={}, name={}", organizationId, name);
        OrganizationEntity organization = commonService.findOrganization(organizationId);
        organization.setName(name);
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
    public OrganizationRole getRole(String organizationId, String roleId) throws OrganizationNotFound {
        log.info("Get organization role: organizationId={}, roleId={}", organizationId, roleId);
        commonService.findOrganization(organizationId);
        OrganizationRoleEntity role = organizationRoleRepository.findByOrganizationIdAndRoleId(organizationId, roleId)
                .orElseThrow(OrganizationNotFound::new);
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

    private List<OrganizationEntity> findOrganizations(ListOrganizationsRequest request, Pageable pageable) {
        String token = request.getContinuationToken();
        OrganizationStatus status = request.getStatus();
        if (status != null && token != null) {
            return organizationRepository.findByStatusAndIdLessThanOrderByIdDesc(status.name(), token, pageable);
        }
        if (status != null) {
            return organizationRepository.findByStatusOrderByIdDesc(status.name(), pageable);
        }
        if (token != null) {
            return organizationRepository.findByIdLessThanOrderByIdDesc(token, pageable);
        }
        return organizationRepository.findAllByOrderByIdDesc(pageable);
    }

    private int organizationLimit(ListOrganizationsRequest request) {
        if (!request.isSetLimit() || request.getLimit() <= 0) {
            return DEFAULT_ORGANIZATION_LIMIT;
        }
        return Math.min(request.getLimit(), MAX_ORGANIZATION_LIMIT);
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
