package dev.vality.orgmanager.converter;

import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.util.JsonMapper;
import dev.vality.swag.organizations.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationConverter {

    private final JsonMapper jsonMapper;

    public OrganizationEntity toEntity(Organization organization, String ownerId) {
        String orgId = UUID.randomUUID().toString();
        return OrganizationEntity.builder()
                .id(orgId)
                .createdAt(LocalDateTime.now())
                .name(organization.getName())
                .owner(ownerId)
                // TODO [ggmaleva]: replace on unique id after introduce separate token generator
                .party(ownerId)
                .metadata(jsonMapper.toJson(organization.getMetadata()))
                .build();
    }

    public Organization toDomain(OrganizationEntity entity) {
        return new Organization()
                .id(entity.getId())
                .createdAt(OffsetDateTime.of(entity.getCreatedAt(), ZoneOffset.UTC))
                .name(entity.getName())
                .owner(entity.getOwner())
                .party(entity.getParty())
                .metadata(entity.getMetadata() != null ? jsonMapper.toMap(entity.getMetadata()) : null);
    }

}
