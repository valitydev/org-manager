package dev.vality.orgmanager.service;

import dev.vality.bouncer.context.v1.User;
import dev.vality.bouncer.ctx.ContextFragment;
import dev.vality.bouncer.ctx.ContextFragmentType;
import dev.vality.orgmanagement.AuthContextProviderSrv;
import dev.vality.orgmanagement.PartyNotFound;
import dev.vality.orgmanager.converter.BouncerContextConverter;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.orgmanager.repository.OrganizationRepository;
import dev.vality.orgmanager.service.model.UserInfo;
import dev.vality.woody.api.trace.ContextUtils;
import dev.vality.woody.api.trace.context.metadata.user.UserIdentityEmailExtensionKit;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthContextService implements AuthContextProviderSrv.Iface {

    private final UserService userService;
    private final BouncerContextConverter bouncerConverter;
    private final OrganizationRepository organizationRepository;

    @Override
    public ContextFragment getUserContext(String id) throws TException {
        dev.vality.bouncer.context.v1.ContextFragment contextFragment =
                new dev.vality.bouncer.context.v1.ContextFragment();
        contextFragment.setUser(getUser(id));
        TSerializer byteSerializer = new TSerializer();
        return new ContextFragment()
                .setType(ContextFragmentType.v1_thrift_binary)
                .setContent(byteSerializer.serialize(contextFragment));
    }

    @Override
    public ContextFragment getPartyContext(String id) throws TException {
        OrganizationEntity organization = organizationRepository.findByParty(id)
                .orElseThrow(PartyNotFound::new);
        dev.vality.bouncer.context.v1.ContextFragment contextFragment =
                new dev.vality.bouncer.context.v1.ContextFragment();
        contextFragment.setParty(bouncerConverter.toParty(organization));
        TSerializer byteSerializer = new TSerializer();
        return new ContextFragment()
                .setType(ContextFragmentType.v1_thrift_binary)
                .setContent(byteSerializer.serialize(contextFragment));
    }

    private User getUser(String id) {
        UserInfo userInfo = userService.findById(id);
        User bouncerUser = bouncerConverter.toUser(userInfo.getMember(), userInfo.getOrganizations());
        if (userInfo.getMember() == null) {
            bouncerUser.setId(id);
            bouncerUser.setEmail(
                    ContextUtils.getCustomMetadataValue(UserIdentityEmailExtensionKit.INSTANCE.getExtension())
            );
        }
        return bouncerUser;
    }
}
