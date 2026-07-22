package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * Mapper para transformar respuestas de API a modelos de dominio
 *
 * Single Responsibility: ÚNICAMENTE mapear respuestas de API a dominio
 * - NO sabe de HTTP
 * - NO sabe de comunicación externa
 * - SOLO transformación de datos pura
 */
@Component
public class InalambriaResponseToDomainMapper {

    /**
     * Mapea respuesta HTTP de API a SmsBatchResult de dominio
     */
    public SmsBatchResult mapToDomainModel(ResponseEntity<HashMap<String, Object>> response) {
        return SmsBatchResult.builder()
                .success(mapSuccess(response))
                .providerReference(extractProviderReference(response.getBody()))
                .batchId(extractBatchId(response.getBody()))
                .acceptedCount(extractAcceptedCount(response.getBody()))
                .rejectedCount(extractRejectedCount(response.getBody()))
                .timestamp(getCurrentTimestamp())
                .metadata(mapMetadata(response))
                .build();
    }

    /**
     * Determina si la respuesta es exitosa
     */
    private boolean mapSuccess(ResponseEntity<HashMap<String, Object>> response) {
        return response != null && response.getStatusCode().is2xxSuccessful();
    }

    /**
     * Extrae providerReference de respuesta
     */
    private String extractProviderReference(HashMap<String, Object> body) {
        return extractStringValue(body, "providerReference");
    }

    /**
     * Extrae batchId de respuesta
     */
    private String extractBatchId(HashMap<String, Object> body) {
        return extractStringValue(body, "batchId");
    }

    /**
     * Extrae acceptedCount de respuesta
     */
    private int extractAcceptedCount(HashMap<String, Object> body) {
        return extractIntValue(body, "acceptedCount");
    }

    /**
     * Extrae rejectedCount de respuesta
     */
    private int extractRejectedCount(HashMap<String, Object> body) {
        return extractIntValue(body, "rejectedCount");
    }

    /**
     * Extrae valor string de mapa de respuesta
     */
    private String extractStringValue(HashMap<String, Object> body, String key) {
        if (body == null || !body.containsKey(key)) {
            return null;
        }
        Object value = body.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Extrae valor int de mapa de respuesta
     */
    private int extractIntValue(HashMap<String, Object> body, String key) {
        if (body == null || !body.containsKey(key)) {
            return 0;
        }
        Object value = body.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Obtiene timestamp actual
     */
    private LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }

    /**
     * Mapea metadatos de la respuesta
     */
    private HashMap<String, Object> mapMetadata(ResponseEntity<HashMap<String, Object>> response) {
        HashMap<String, Object> metadata = new java.util.HashMap<>();
        if (response != null) {
            metadata.put("rawResponse", response.getBody());
            metadata.put("httpStatus", response.getStatusCode().value());
        }
        return metadata;
    }
}
