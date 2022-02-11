package dev.vality.orgmanager.service;

import dev.vality.orgmanager.entity.MemberEntity;

import java.util.Optional;

public interface MemberService {

    Optional<MemberEntity> findById(String id);

}
