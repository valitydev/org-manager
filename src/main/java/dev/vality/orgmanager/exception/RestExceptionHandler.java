package dev.vality.orgmanager.exception;

import dev.vality.swag.organizations.model.JoinOrg422Response;
import dev.vality.swag.organizations.model.RemoveMemberRole422Response;
import dev.vality.swag.organizations.model.RevokeInvitation422Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {AccessDeniedException.class})
    protected ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build();
    }

    @ExceptionHandler(value = {ResourceNotFoundException.class})
    protected ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build();
    }

    @ExceptionHandler(value = {BouncerException.class})
    protected ResponseEntity<Object> handleBouncerException(BouncerException ex, WebRequest request) {
        log.error(ex.getMessage(), ex.getCause());
        return ResponseEntity
                .status(HttpStatus.FAILED_DEPENDENCY)
                .build();
    }

    @ExceptionHandler(InviteExpiredException.class)
    public ResponseEntity<JoinOrg422Response> handleInviteExpiredException(InviteExpiredException ex) {
        JoinOrg422Response badResponse = new JoinOrg422Response()
                .code(JoinOrg422Response.CodeEnum.INVITATION_EXPIRED)
                .message(String.format("Invite expired at: %s", ex.getExpiredAt()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(badResponse);
    }

    @ExceptionHandler(InviteRevokedException.class)
    public ResponseEntity<RevokeInvitation422Response> handleInviteRevokedException(InviteRevokedException ex) {
        RevokeInvitation422Response badResponse = new RevokeInvitation422Response()
                .code(RevokeInvitation422Response.CodeEnum.INVALID_STATUS)
                .message(String.format("Invite revoked: %s", ex.getReason()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(badResponse);
    }

    @ExceptionHandler(InviteAlreadyAcceptedException.class)
    public ResponseEntity<JoinOrg422Response> handleInviteAlreadyAcceptedException(InviteAlreadyAcceptedException ex) {
        JoinOrg422Response badResponse = new JoinOrg422Response()
                .code(JoinOrg422Response.CodeEnum.INVITATION_EXPIRED)
                .message(String.format("Invite accepted at: %s", ex.getAcceptedAt()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(badResponse);
    }

    @ExceptionHandler(LastRoleException.class)
    public ResponseEntity<RemoveMemberRole422Response> handleLastRoleException(LastRoleException ex) {
        RemoveMemberRole422Response badResponse = new RemoveMemberRole422Response()
                .code(RemoveMemberRole422Response.CodeEnum.ONLY_ROLE_LEFT)
                .message("Member have only one role");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(badResponse);
    }

}
