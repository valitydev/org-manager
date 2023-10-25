package dev.vality.orgmanager.converter;

import dev.vality.bouncer.base.Entity;
import dev.vality.bouncer.context.v1.OrgRole;
import dev.vality.bouncer.context.v1.OrgRoleScope;
import dev.vality.bouncer.context.v1.Organization;
import dev.vality.bouncer.context.v1.User;
import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BouncerContextConverter {

    public User toUser(MemberEntity member, Set<OrganizationEntity> organizations) {
        return new User()
                .setId(member == null ? null : member.getId())
                .setRealm(new Entity())
                .setEmail(member == null ? null : member.getEmail())
                .setOrgs(
                        organizations.stream()
                                .map(organizationEntity -> this.toOrganization(
                                        organizationEntity,
                                        member == null ? null : member.getRoles())
                                )
                                .collect(Collectors.toSet())
                );
    }

    public Organization toOrganization(OrganizationEntity entity,
                                       Set<MemberRoleEntity> roles) {
        return new Organization()
                .setId(entity.getId())
                .setOwner(new Entity().setId(entity.getOwner()))
                .setParty(new Entity().setId(entity.getParty()))
                .setRoles(CollectionUtils.isEmpty(roles) ? null :
                        roles.stream()
                                .filter(memberRoleEntity -> memberRoleEntity.getOrganizationId().equals(entity.getId()))
                                .map(this::toOrgRole)
                                .collect(Collectors.toSet()));
    }

    public OrgRole toOrgRole(MemberRoleEntity entity) {
        return new OrgRole()
                .setId(entity.getRoleId())
                .setScope(Objects.nonNull(entity.getResourceId())
                        ? new OrgRoleScope()
                        .setShop(
                                new Entity()
                                        .setId(entity.getResourceId()))
                        : null);

    }
}
