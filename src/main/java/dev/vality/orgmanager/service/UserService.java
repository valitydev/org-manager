package dev.vality.orgmanager.service;

import dev.vality.orgmanager.service.model.UserInfo;

public interface UserService {

    UserInfo findById(String id);

}
