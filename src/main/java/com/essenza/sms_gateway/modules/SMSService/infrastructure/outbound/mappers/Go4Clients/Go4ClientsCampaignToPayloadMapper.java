package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.Go4Clients;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper MapStruct para transformar comandos de campaña a payloads de API de Go4Clients
 * 
 * Single Responsibility: ÚNICAMENTE mapear datos entre dominio y API usando MapStruct
 * - NO sabe de HTTP
 * - NO sabe de comunicación externa
 * - SOLO transformación de datos pura generada por MapStruct
 */
@Mapper(componentModel = "spring")
public interface Go4ClientsCampaignToPayloadMapper {

    /**
     * Mapea CreateSmsCampaignCommand a payload específico de API
     */
    default HashMap<String, Object> mapToApiPayload(CreateSmsCampaignCommand command) {
        HashMap<String, Object> payload = new HashMap<>();
        
        // Mapear campos básicos de campaña
        payload.put("name", command.getName());
        payload.put("sender", command.getSender());
        
        return payload;
    }

    /**
     * Mapea SendSmsBatchCommand a payload de envío batch
     */
    default HashMap<String, Object> mapToBatchPayload(SendSmsBatchCommand command) {
        HashMap<String, Object> payload = new HashMap<>();
        
        // Mapear campos de batch
        payload.put("batchId", command.getCampaignId());
        payload.put("message", command.getMessage());
        payload.put("recipients", mapDestinations(command.getDestinations()));
        
        // Mapear campos opcionales
        if (command.getPriority() != null) {
            payload.put("priority", command.getPriority().name());
        }
        if (command.getScheduledDate() != null) {
            payload.put("scheduledDate", command.getScheduledDate().toString());
        }
        if (command.getLandingCustomFields() != null) {
            payload.put("landingCustomFields", command.getLandingCustomFields());
        }
        
        return payload;
    }

    /**
     * Mapea destinatarios de campaña a formato de API
     */
    @Named("mapRecipients")
    default HashMap<String, Object> mapRecipients(Map<String, String> recipients) {
        if (recipients == null) {
            return new HashMap<>();
        }
        
        HashMap<String, Object> apiRecipients = new HashMap<>();
        recipients.forEach((phone, name) -> {
            HashMap<String, String> recipient = new HashMap<>();
            recipient.put("phone", phone);
            recipient.put("name", name);
            apiRecipients.put(phone, recipient);
        });
        return apiRecipients;
    }

    /**
     * Mapea destinatarios de batch a formato de API
     */
    @Named("mapDestinations")
    default HashMap<String, Object> mapDestinations(java.util.List<com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.DestinatarioDto> destinations) {
        if (destinations == null) {
            return new HashMap<>();
        }
        
        HashMap<String, Object> apiDestinations = new HashMap<>();
        destinations.forEach(dest -> {
            HashMap<String, String> recipient = new HashMap<>();
            String phone = dest.getPhoneNumber();
            String countryCode = dest.getCountryCode() != null ? dest.getCountryCode() : "57";
            recipient.put("phone", phone);
            recipient.put("countryCode", countryCode);
            recipient.put("name", "RECIPIENT_" + phone.substring(Math.max(0, phone.length() - 4)));
            apiDestinations.put(phone, recipient);
        });
        return apiDestinations;
    }

    /**
     * Mapea fecha programada a string
     */
    @Named("mapScheduledDate")
    default String mapScheduledDate(Object scheduledDate) {
        return scheduledDate != null ? scheduledDate.toString() : null;
    }
}
