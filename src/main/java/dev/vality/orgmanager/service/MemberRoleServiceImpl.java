package dev.vality.orgmanager.service;

import dev.vality.orgmanager.converter.MemberRoleConverter;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.exception.ResourceNotFoundException;
import dev.vality.orgmanager.repository.MemberRoleRepository;
import dev.vality.swag.organizations.model.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberRoleServiceImpl implements MemberRoleService {

    private final MemberRoleRepository repository;
    private final MemberRoleConverter converter;

    @Override
    @Transactional(readOnly = true)
    public MemberRole findById(String id) {
        return repository.findById(id)
                .map(converter::toDomain)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberRoleEntity findEntityById(String id) {
        return repository.findById(id)
                .orElseThrow(ResourceNotFoundException::new);
    }

}
