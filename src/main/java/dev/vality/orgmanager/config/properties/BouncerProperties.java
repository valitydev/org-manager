package dev.vality.orgmanager.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "bouncer")
@Data
public class BouncerProperties {

    private String contextFragmentId;
    private String deploymentId;
    private String authMethod;
    private String realm;
    private String ruleSetId;
    private List<String> requesterIpHeaders = List.of("CF-Connecting-IP", "X-Forwarded-For");

}
