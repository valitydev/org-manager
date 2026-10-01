package dev.vality.orgmanager.util;

import dev.vality.orgmanager.exception.InvalidAllowedIpException;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class AllowedIps {

    private static final InetAddressValidator IP_VALIDATOR = InetAddressValidator.getInstance();

    private AllowedIps() {
    }

    public static Set<String> normalize(Collection<String> values) {
        Set<String> result = new HashSet<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            result.add(value == null ? null : value.strip());
        }
        return result;
    }

    public static void validate(Collection<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (!isIpAddress(value)) {
                throw new InvalidAllowedIpException(
                        "Allowed ip is expected to be an IP address, but was: '" + value + "'");
            }
        }
    }

    private static boolean isIpAddress(String value) {
        return value != null
                && value.indexOf('/') < 0
                && value.indexOf('%') < 0
                && IP_VALIDATOR.isValid(value);
    }
}
