package dev.vality.orgmanager.service;

import dev.vality.orgmanager.config.properties.BouncerProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequesterIpResolverTest {

    private final BouncerProperties bouncerProperties = new BouncerProperties();
    private final RequesterIpResolver resolver = new RequesterIpResolver(bouncerProperties);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        request.setRemoteAddr("10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void firstConfiguredHeaderWins() {
        request.addHeader("X-Forwarded-For", "1.1.1.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.7");

        assertEquals(Optional.of("203.0.113.7"), resolver.resolve());
    }

    @Test
    void leftmostForwardedForEntryIsUsed() {
        request.addHeader("X-Forwarded-For", "203.0.113.7, 162.158.1.1, 172.16.0.1");

        assertEquals(Optional.of("203.0.113.7"), resolver.resolve());
    }

    @Test
    void remoteAddressIsUsedWithoutHeaders() {
        assertEquals(Optional.of("10.0.0.1"), resolver.resolve());
    }

    @Test
    void configuredHeadersAreUsed() {
        bouncerProperties.setRequesterIpHeaders(List.of("X-Real-IP"));
        request.addHeader("CF-Connecting-IP", "198.51.100.1");
        request.addHeader("X-Real-IP", "203.0.113.7");

        assertEquals(Optional.of("203.0.113.7"), resolver.resolve());
    }

    @Test
    void emptyOnError() {
        bouncerProperties.setRequesterIpHeaders(null);

        assertEquals(Optional.empty(), resolver.resolve());
    }

    @Test
    void emptyOutsideOfHttpRequest() {
        RequestContextHolder.resetRequestAttributes();

        assertEquals(Optional.empty(), resolver.resolve());
    }
}
