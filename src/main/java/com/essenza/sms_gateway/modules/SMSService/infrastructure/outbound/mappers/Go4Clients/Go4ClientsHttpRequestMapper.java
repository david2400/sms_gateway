package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.Go4Clients;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper MapStruct para construir peticiones HTTP para Go4Clients
 * 
 * Single Responsibility: ÚNICAMENTE mapear datos a peticiones HTTP usando MapStruct
 * - NO sabe de comunicación externa
 * - NO sabe de ejecución HTTP
 * - SOLO construcción de peticiones pura generada por MapStruct
 */
@Mapper(componentModel = "spring")
public interface Go4ClientsHttpRequestMapper {

    /**
     * Mapea payload y token a HttpEntity para petición HTTP
     */
    default HttpEntity<Object> mapToHttpRequest(HashMap<String, Object> payload, String token) {
        HttpHeaders headers = buildCampaignHeaders(token);
        return new HttpEntity<>(payload, headers);
    }

    /**
     * Mapea payload con autenticación Go4Clients (API Key + API Secret)
     */
    default HttpEntity<Object> mapToHttpRequestWithApiKeys(HashMap<String, Object> payload, String apiKey, String apiSecret) {
        HttpHeaders headers = buildGo4ClientsHeaders(apiKey, apiSecret);
        return new HttpEntity<>(payload, headers);
    }

    /**
     * Mapea payload con autenticación Go4Clients (API Key + API Secret)
     */
    @Named("buildGo4ClientsHeaders")
    default HttpHeaders buildGo4ClientsHeaders(String apiKey, String apiSecret) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey);
        headers.set("X-API-SECRET", apiSecret);
        headers.set("X-API-Version", "v1");
        return headers;
    }

    /**
     * Construye headers para petición de campaña
     */
    @Named("buildCampaignHeaders")
    default HttpHeaders buildCampaignHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Campaign-Source", "SMS-SERVICE");
        return headers;
    }

    /**
     * Construye URL completa del endpoint
     */
    @Named("mapToEndpointUrl")
    default String mapToEndpointUrl(String baseUrl, String path) {
        if (path.startsWith("http")) {
            return path;
        }
        return baseUrl + path;
    }
}
