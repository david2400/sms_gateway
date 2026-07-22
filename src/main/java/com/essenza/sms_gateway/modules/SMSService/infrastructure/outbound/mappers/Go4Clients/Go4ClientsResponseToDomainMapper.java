package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.Go4Clients;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignOperationResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper MapStruct para transformar respuestas de API a modelos de dominio
 * 
 * Single Responsibility: ÚNICAMENTE mapear respuestas de API a dominio usando MapStruct
 * - NO sabe de HTTP
 * - NO sabe de comunicación externa
 * - SOLO transformación de datos pura generada por MapStruct
 */
@Mapper(componentModel = "spring", imports = {LocalDateTime.class})
public interface Go4ClientsResponseToDomainMapper {

    /**
     * Mapea respuesta HTTP de API a SmsCampaignOperationResult
     */
    @Mapping(target = "success", source = "response", qualifiedByName = "mapSuccess")
    @Mapping(target = "campaignId", source = "response.body", qualifiedByName = "extractCampaignId")
    @Mapping(target = "providerReference", source = "response.body", qualifiedByName = "extractProviderReference")
    @Mapping(target = "status", source = "response.body", qualifiedByName = "extractStatus")
    @Mapping(target = "timestamp", expression = "java(getCurrentTimestamp())")
    @Mapping(target = "metadata", source = "response", qualifiedByName = "mapMetadata")
    SmsCampaignOperationResult mapToCampaignResult(ResponseEntity<HashMap<String, Object>> response);

    /**
     * Mapea respuesta HTTP de API a SmsBatchResult
     */
    @Mapping(target = "success", source = "response", qualifiedByName = "mapSuccess")
    @Mapping(target = "providerReference", source = "response.body", qualifiedByName = "extractProviderReference")
    @Mapping(target = "batchId", source = "response.body", qualifiedByName = "extractBatchId")
    @Mapping(target = "acceptedCount", source = "response.body", qualifiedByName = "extractAcceptedCount")
    @Mapping(target = "rejectedCount", source = "response.body", qualifiedByName = "extractRejectedCount")
    @Mapping(target = "timestamp", expression = "java(getCurrentTimestamp())")
    @Mapping(target = "metadata", source = "response", qualifiedByName = "mapMetadata")
    SmsBatchResult mapToBatchResult(ResponseEntity<HashMap<String, Object>> response);

    /**
     * Mapea respuesta HTTP de API a SmsCampaignReport
     */
    @Mapping(target = "campaignId", source = "response.body", qualifiedByName = "extractCampaignId")
    @Mapping(target = "status", source = "response.body", qualifiedByName = "extractStatus")
    @Mapping(target = "delivered", source = "response.body", qualifiedByName = "extractDelivered")
    @Mapping(target = "failed", source = "response.body", qualifiedByName = "extractFailed")
    @Mapping(target = "pending", source = "response.body", qualifiedByName = "extractPending")
    @Mapping(target = "total", source = "response.body", qualifiedByName = "extractTotal")
    @Mapping(target = "generatedAt", expression = "java(getCurrentTimestamp())")
    @Mapping(target = "metadata", source = "response", qualifiedByName = "mapMetadata")
    SmsCampaignReport mapToCampaignReport(ResponseEntity<HashMap<String, Object>> response);

    /**
     * Determina si la respuesta es exitosa
     */
    @Named("mapSuccess")
    default boolean mapSuccess(ResponseEntity<HashMap<String, Object>> response) {
        return response != null && response.getStatusCode().is2xxSuccessful();
    }

    /**
     * Extrae campaignId de respuesta
     */
    @Named("extractCampaignId")
    default String extractCampaignId(HashMap<String, Object> body) {
        return extractStringValue(body, "campaignId");
    }

    /**
     * Extrae providerReference de respuesta
     */
    @Named("extractProviderReference")
    default String extractProviderReference(HashMap<String, Object> body) {
        return extractStringValue(body, "providerReference");
    }

    /**
     * Extrae status de respuesta
     */
    @Named("extractStatus")
    default String extractStatus(HashMap<String, Object> body) {
        return extractStringValue(body, "status");
    }

    /**
     * Extrae batchId de respuesta
     */
    @Named("extractBatchId")
    default String extractBatchId(HashMap<String, Object> body) {
        return extractStringValue(body, "batchId");
    }

    /**
     * Extrae acceptedCount de respuesta
     */
    @Named("extractAcceptedCount")
    default int extractAcceptedCount(HashMap<String, Object> body) {
        return extractIntValue(body, "acceptedCount");
    }

    /**
     * Extrae rejectedCount de respuesta
     */
    @Named("extractRejectedCount")
    default int extractRejectedCount(HashMap<String, Object> body) {
        return extractIntValue(body, "rejectedCount");
    }

    /**
     * Extrae totalSent de respuesta
     */
    @Named("extractTotalSent")
    default int extractTotalSent(HashMap<String, Object> body) {
        return extractIntValue(body, "totalSent");
    }

    /**
     * Extrae totalDelivered de respuesta
     */
    @Named("extractDelivered")
    default int extractDelivered(HashMap<String, Object> body) {
        return extractIntValue(body, "delivered");
    }

    /**
     * Extrae totalFailed de respuesta
     */
    @Named("extractFailed")
    default int extractFailed(HashMap<String, Object> body) {
        return extractIntValue(body, "failed");
    }

    /**
     * Extrae pending de respuesta
     */
    @Named("extractPending")
    default int extractPending(HashMap<String, Object> body) {
        return extractIntValue(body, "pending");
    }

    /**
     * Extrae total de respuesta
     */
    @Named("extractTotal")
    default int extractTotal(HashMap<String, Object> body) {
        return extractIntValue(body, "total");
    }

    /**
     * Extrae valor string de mapa de respuesta
     */
    default String extractStringValue(HashMap<String, Object> body, String key) {
        if (body == null || !body.containsKey(key)) {
            return null;
        }
        Object value = body.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Extrae valor int de mapa de respuesta
     */
    @Named("extractIntValue")
    default int extractIntValue(HashMap<String, Object> body, String key) {
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
    @Named("getCurrentTimestamp")
    default LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }

    /**
     * Mapea metadatos de la respuesta
     */
    @Named("mapMetadata")
    default HashMap<String, Object> mapMetadata(ResponseEntity<HashMap<String, Object>> response) {
        HashMap<String, Object> metadata = new java.util.HashMap<>();
        if (response != null) {
            metadata.put("rawResponse", response.getBody());
            metadata.put("httpStatus", response.getStatusCode().value());
        }
        return metadata;
    }
}
