package dev.vality.orgmanager.exception;

import lombok.Getter;

@Getter
public class InviteRevokedException extends RuntimeException {

    private final String reason;

    public InviteRevokedException(String reason) {
        super();
        this.reason = reason;
    }

}
