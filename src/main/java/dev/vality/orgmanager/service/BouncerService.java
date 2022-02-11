package dev.vality.orgmanager.service;

import dev.vality.orgmanager.service.dto.BouncerContextDto;

public interface BouncerService {

    boolean havePrivileges(BouncerContextDto bouncerContext);
}
