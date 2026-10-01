package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.CreateOrganizationRequest;
import dev.vality.orgmanagement.InvalidOrganizationState;
import dev.vality.orgmanagement.InvalidRequest;
import dev.vality.orgmanagement.ListOrganizationsRequest;
import dev.vality.orgmanagement.ListOrganizationsResult;
import dev.vality.orgmanagement.ModifyOrganizationRequest;
import dev.vality.orgmanagement.Organization;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.PartyAlreadyBound;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.entity.StoredOrganizationStatus;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.service.dto.AdminPage;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.vality.orgmanager.service.AdminCommonService.pageLimit;
import static java.util.Objects.requireNonNullElseGet;

/**
 * Организации в административном контракте.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AdminManagementConverter converter;
    private final AdminCommonService commonService;

    @Transactional
    public Organization create(CreateOrganizationRequest request) throws PartyAlreadyBound, InvalidRequest {
        log.info("Create organization: partyId={}, ownerId={}", request.getPartyId(), request.getOwnerId());
        String partyId = commonService.requireText(request.getPartyId(), "Party id");
        commonService.requireText(request.getOwnerId(), "Owner id");
        String name = commonService.requireText(request.getName(), "Organization name");
        String metadata = commonService.toStoredMetadata(request.getMetadata());
        Set<String> allowedIps = commonService.toAllowedIps(request.getAllowedIps());
        organizationRepository.lockByParty(partyId);
        if (organizationRepository.existsByParty(partyId)) {
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
                .allowedIps(allowedIps)
                .build();
        return converter.toOrganization(organizationRepository.saveAndFlush(entity));
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
        ListOrganizationsRequest safeRequest = requireNonNullElseGet(request, ListOrganizationsRequest::new);
        int limit = pageLimit(safeRequest.getLimit());
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "id"));
        AdminPage<OrganizationEntity> page = AdminPage.of(
                organizationRepository.findAll(specification(safeRequest), pageable).getContent(),
                limit,
                OrganizationEntity::getId);
        ListOrganizationsResult result = new ListOrganizationsResult(
                page.items().stream().map(converter::toOrganization).toList());
        page.continuationToken().ifPresent(result::setContinuationToken);
        return result;
    }

    @Transactional
    public Organization modify(String organizationId, ModifyOrganizationRequest request)
            throws OrganizationNotFound, InvalidRequest {
        log.info("Modify organization: organizationId={}, request={}", organizationId, request);
        ModifyOrganizationRequest safeRequest = request == null ? new ModifyOrganizationRequest() : request;
        OrganizationEntity organization = commonService.lockOrganization(organizationId);
        String name = safeRequest.isSetName()
                ? commonService.requireText(safeRequest.getName(), "Organization name")
                : organization.getName();
        String metadata = safeRequest.isSetMetadata()
                ? commonService.toStoredMetadata(safeRequest.getMetadata())
                : organization.getMetadata();
        Set<String> allowedIps = safeRequest.isSetAllowedIps()
                ? commonService.toAllowedIps(safeRequest.getAllowedIps())
                : organization.getAllowedIps();
        organization.setName(name);
        organization.setMetadata(metadata);
        organization.setAllowedIps(allowedIps);
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
        OrganizationEntity organization = commonService.lockOrganization(organizationId);
        String current = organization.getStatus();
        if (!expected.matches(current)) {
            throw new InvalidOrganizationState(
                    "Expected organization status " + expected.getValue() + ", but was " + current);
        }
        organization.setStatus(target.getValue());
        return converter.toOrganization(organizationRepository.save(organization));
    }
}
