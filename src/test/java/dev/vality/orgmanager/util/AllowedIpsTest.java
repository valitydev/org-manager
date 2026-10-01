package dev.vality.orgmanager.util;

import dev.vality.orgmanager.exception.InvalidAllowedIpException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedIpsTest {

    @Test
    void normalizeShouldTrimAndDeduplicate() {
        assertEquals(Set.of("1.2.3.4", "10.0.0.1"),
                AllowedIps.normalize(List.of(" 1.2.3.4", "1.2.3.4 ", "10.0.0.1")));
    }

    @Test
    void normalizeShouldNotValidate() {
        assertEquals(Set.of("not-an-ip", ""), AllowedIps.normalize(List.of(" not-an-ip ", "  ")));
    }

    @Test
    void normalizeShouldReturnEmptySetForNull() {
        assertTrue(AllowedIps.normalize(null).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.2.3.4", "0.0.0.0", "255.255.255.255", "::1", "::", "2001:db8::1", "2001:DB8::1", "::ffff:1.2.3.4"
    })
    void validateShouldAcceptIpAddress(String value) {
        assertDoesNotThrow(() -> AllowedIps.validate(List.of(value)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ", " 1.2.3.4", "1.2.3.4 ", "localhost", "example.com", "1.2.3", "256.1.1.1", "010.0.0.1",
            "10.0.0.0/8", "1.2.3.4/32", "2001:db8::/32", "::1/128", "fe80::1%eth0", "[::1]", "2001:db8:::1",
            "1.2.3.4 5.6.7.8"
    })
    void validateShouldRejectInvalidValue(String value) {
        assertThrows(InvalidAllowedIpException.class, () -> AllowedIps.validate(Arrays.asList(value)));
    }

    @Test
    void validateShouldAcceptNull() {
        assertDoesNotThrow(() -> AllowedIps.validate(null));
    }
}
