package com.essenza.sms_gateway.modules.SMSService.infrastructure.config;

import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.inalambria.EmptyArraySerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class SmsHttpClientConfig {

    private final SmsProvidersProperties smsProviderProperties;

    @Bean(name = "go4ClientsRestTemplate")
    public RestTemplate go4ClientsRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean(name = "inalambriaRestTemplate")
    public RestTemplate inalambriaRestTemplate(RestTemplateBuilder builder) {
        // RestTemplate simple sin serializador personalizado
        // ArrayList serializa naturalmente como [] sin configuración especial
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
