package com.essenza.sms_gateway.modules.SmsCore.shared;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entrega un {@link RestTemplate} por combinación de timeouts (vienen de {@code sms_proveedor}).
 * Lo comparten los módulos de proveedor; las instancias se reutilizan (RestTemplate es thread-safe).
 * <p>
 * Usa los convertidores por defecto de Spring (sin la estrategia SNAKE_CASE global de la app),
 * así el payload enviado a cada proveedor es exactamente el que arma su mapper.
 */
@Component
public class ProviderRestTemplateFactory {

    private final Map<String, RestTemplate> templates = new ConcurrentHashMap<>();

    public RestTemplate get(int connectTimeoutMs, int readTimeoutMs) {
        int connect = connectTimeoutMs > 0 ? connectTimeoutMs : 2000;
        int read = readTimeoutMs > 0 ? readTimeoutMs : 5000;
        return templates.computeIfAbsent(connect + "|" + read, k -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(connect);
            factory.setReadTimeout(read);
            return new RestTemplate(factory);
        });
    }
}
