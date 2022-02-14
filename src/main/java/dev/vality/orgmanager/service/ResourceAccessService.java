package dev.vality.orgmanager.service;

import dev.vality.orgmanager.service.dto.ResourceDto;

public interface ResourceAccessService {

    void checkRights();

    void checkRights(ResourceDto resource);

}
