package dev.vality.orgmanager.service;

import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.swag.organizations.model.MemberRole;

public interface MemberRoleService {

    MemberRole findById(String id);

    MemberRoleEntity findEntityById(String id);


}
