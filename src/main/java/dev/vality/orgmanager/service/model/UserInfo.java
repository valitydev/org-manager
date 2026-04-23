package dev.vality.orgmanager.service.model;

import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.annotation.Nullable;

import java.util.Set;

@Data
@AllArgsConstructor
public class UserInfo {
    @Nullable
    private MemberEntity member;
    private Set<OrganizationEntity> organizations;
}
