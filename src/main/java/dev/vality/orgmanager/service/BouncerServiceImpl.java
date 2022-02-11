package dev.vality.orgmanager.service;

import dev.vality.bouncer.decisions.ArbiterSrv;
import dev.vality.bouncer.decisions.Context;
import dev.vality.bouncer.decisions.Judgement;
import dev.vality.bouncer.decisions.Resolution;
import dev.vality.orgmanagement.UserNotFound;
import dev.vality.orgmanager.config.properties.BouncerProperties;
import dev.vality.orgmanager.exception.BouncerException;
import dev.vality.orgmanager.service.dto.BouncerContextDto;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BouncerServiceImpl implements BouncerService {

    private final BouncerContextFactory bouncerContextFactory;
    private final ArbiterSrv.Iface bouncerClient;
    private final BouncerProperties bouncerProperties;

    @Override
    public boolean havePrivileges(BouncerContextDto bouncerContext) {
        try {
            Context context = bouncerContextFactory.buildContext(bouncerContext);
            Judgement judge = bouncerClient.judge(bouncerProperties.getRuleSetId(), context);
            Resolution resolution = judge.getResolution();
            return resolution.isSetAllowed();
        } catch (UserNotFound e) {
            throw new BouncerException("Error while build bouncer context", e);
        } catch (TException e) {
            throw new BouncerException("Error while call bouncer", e);
        }
    }
}
