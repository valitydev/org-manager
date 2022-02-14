package dev.vality.orgmanager.config;

import dev.vality.bouncer.decisions.ArbiterSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class BouncerConfig {

    @Bean
    public ArbiterSrv.Iface bouncerClient(@Value("${bouncer.url}") Resource resource,
                                          @Value("${bouncer.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI())
                .build(ArbiterSrv.Iface.class);
    }

}
