package dev.vality.orgmanager.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.vality.orgmanager.controller.converter.InvitationStatusConverter;
import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.FormattingConversionService;

@Configuration
public class AppConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperCustomizer() {
        return builder -> builder
                .changeDefaultPropertyInclusion(inclusion ->
                        inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
                .addModule(new JsonNullableJackson3Module());
    }

    @Autowired
    public void fillWebConverter(FormattingConversionService formatService) {
        formatService.addConverter(new InvitationStatusConverter());
    }

}
