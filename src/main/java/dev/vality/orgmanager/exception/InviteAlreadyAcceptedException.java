package dev.vality.orgmanager.exception;

import lombok.Getter;

@Getter
public class InviteAlreadyAcceptedException extends RuntimeException {

    private String acceptedAt;

    public InviteAlreadyAcceptedException(String acceptedAt) {
        super();
        this.acceptedAt = acceptedAt;
    }
}
