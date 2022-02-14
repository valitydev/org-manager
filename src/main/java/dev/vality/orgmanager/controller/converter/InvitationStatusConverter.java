package dev.vality.orgmanager.controller.converter;

import dev.vality.swag.organizations.model.InvitationStatusName;
import org.springframework.core.convert.converter.Converter;

public class InvitationStatusConverter implements Converter<String, InvitationStatusName> {

    @Override
    public InvitationStatusName convert(String value) {
        return InvitationStatusName.fromValue(value);
    }
}
