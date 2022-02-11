package dev.vality.orgmanager.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvitationDto {

    private String invitationId;
    private String email;
}
