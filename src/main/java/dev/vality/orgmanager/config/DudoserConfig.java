package dev.vality.orgmanager.config;

import dev.vality.damsel.message_sender.MessageSenderSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class DudoserConfig {

    @Bean
    public MessageSenderSrv.Iface dudoserSrv(@Value("${dudoser.url}") Resource resource,
                                             @Value("${dudoser.networkTimeout}") int networkTimeout)
            throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(MessageSenderSrv.Iface.class);
    }
}
