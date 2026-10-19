package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Mapper para construir peticiones HTTP
 *
 * Single Responsibility: ÚNICAMENTE mapear datos a peticiones HTTP
 * - NO sabe de comunicación externa
 * - NO sabe de ejecución HTTP
 * - SOLO construcción de peticiones pura
 */
@Component
public class InalambriaHttpRequestMapper {

    /**
     * Mapea payload y credenciales a HttpEntity para petición HTTP
     */
    public HttpEntity<Object> mapToHttpRequest(HashMap<String, Object> payload, String username, String password) {
        HttpHeaders headers = buildHeaders(username, password);
        return new HttpEntity<>(payload, headers);
    }

    /**
     * Construye headers para petición HTTP con autenticación básica
     */
    private HttpHeaders buildHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);
        return headers;
    }

    /**
     * Versión legacy para compatibilidad (usará username como token)
     */
    public HttpEntity<Object> mapToHttpRequest(HashMap<String, Object> payload, String token) {
        // Si token contiene ":", asumir que es username:password
        if (token != null && token.contains(":")) {
            String[] parts = token.split(":", 2);
            return mapToHttpRequest(payload, parts[0], parts[1]);
        }

        // Si no, usar como Bearer token (para APIs que lo requieran)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(payload, headers);
    }

    /**
     * Construye URL completa del endpoint
     */
    public String mapToEndpointUrl(String baseUrl, String path) {
        if (path.startsWith("http")) {
            return path;
        }
        return baseUrl + path;
    }
}
