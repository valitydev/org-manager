package dev.vality.orgmanager.converter;

import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.util.JsonCodec;
import dev.vality.swag.organizations.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationConverter {

    private final JsonCodec jsonCodec;

    public OrganizationEntity toEntity(Organization organization, String ownerId) {
        String orgId = UUID.randomUUID().toString();
        String partyId = UUID.randomUUID().toString();
        return OrganizationEntity.builder()
                .id(orgId)
                .createdAt(LocalDateTime.now())
                .name(organization.getName())
                .owner(ownerId)
                .party(partyId)
                .metadata(jsonCodec.toJson(organization.getMetadata()))
                .allowedIps(organization.getAllowedIps() == null
                        ? new HashSet<>()
                        : new HashSet<>(organization.getAllowedIps()))
                .build();
    }

    public Organization toDomain(OrganizationEntity entity) {
        return new Organization()
                .id(entity.getId())
                .createdAt(OffsetDateTime.of(entity.getCreatedAt(), ZoneOffset.UTC))
                .name(entity.getName())
                .owner(entity.getOwner())
                .party(entity.getParty())
                .metadata(entity.getMetadata() != null ? jsonCodec.toMap(entity.getMetadata()) : null)
                .allowedIps(CollectionUtils.isEmpty(entity.getAllowedIps())
                        ? null
                        : new HashSet<>(entity.getAllowedIps()));
    }

}
