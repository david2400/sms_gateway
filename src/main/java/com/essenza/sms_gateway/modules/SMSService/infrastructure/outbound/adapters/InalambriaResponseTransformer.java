package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.inalambria.InalambriaResponseToDomainMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import java.util.Map;

/**
 * Especialista en transformar respuestas de API a modelos de dominio
 * 
 * Single Responsibility: ÚNICAMENTE orquestar transformación de respuestas
 * - NO sabe de HTTP
 * - NO sabe de construcción de payloads
 * - DELEGA el mapeo al mapper MapStruct especializado
 */
@Slf4j
@Component
public class InalambriaResponseTransformer {

    private final InalambriaResponseToDomainMapper mapper;

    public InalambriaResponseTransformer(InalambriaResponseToDomainMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Transforma respuesta HTTP de API a modelo de dominio usando MapStruct
     */
    public SmsBatchResult transformToDomainModel(ResponseEntity<HashMap<String, Object>> response) {
        log.debug("[RESPONSE-TRANSFORMER] Delegando transformación al mapper MapStruct");
        
        // Delegar el mapeo al mapper MapStruct
        return mapper.mapToDomainModel(response);
    }
}
