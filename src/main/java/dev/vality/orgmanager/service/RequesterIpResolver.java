package dev.vality.orgmanager.service;

import dev.vality.orgmanager.config.properties.BouncerProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Client IP of the current request: the first configured header that is present wins
 * (leftmost entry for lists like X-Forwarded-For), otherwise the remote address.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequesterIpResolver {

    private final BouncerProperties bouncerProperties;

    public Optional<String> resolve() {
        try {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                log.debug("No current http request, requester ip is not resolved");
                return Optional.empty();
            }
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getRemoteAddr();
            String source = "remote address";
            for (String header : bouncerProperties.getRequesterIpHeaders()) {
                String value = request.getHeader(header);
                if (StringUtils.hasText(value)) {
                    ip = value.split(",")[0].strip();
                    source = "header " + header + ": '" + value + "'";
                    break;
                }
            }
            log.info("Requester ip {} resolved from {}", ip, source);
            return Optional.ofNullable(ip);
        } catch (Exception e) {
            log.warn("Failed to resolve requester ip", e);
            return Optional.empty();
        }
    }
}
