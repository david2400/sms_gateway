package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendInalambriaBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.inalambria.InalambriaCommandToPayloadMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Especialista en construir payloads para la API de Inalambria
 * 
 * Single Responsibility: ÚNICAMENTE orquestar construcción de payloads
 * - NO sabe de HTTP
 * - NO sabe de comunicación externa
 * - DELEGA el mapeo al mapper MapStruct especializado
 */
@Slf4j
@Component
public class InalambriaPayloadBuilder {

    private final InalambriaCommandToPayloadMapper mapper;

    public InalambriaPayloadBuilder(InalambriaCommandToPayloadMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Construye payload específico para la API de Inalambria usando MapStruct
     */
    public HashMap<String, Object> buildFromCommand(SendInalambriaBatchCommand command) {
        log.debug("[PAYLOAD-BUILDER] Delegando construcción al mapper MapStruct");
        
        // Delegar el mapeo al mapper MapStruct
        return mapper.mapToApiPayload(command);
    }
}
