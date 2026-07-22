package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendInalambriaBatchCommand;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper para transformar comandos de dominio a payloads de API de Inalambria
 *
 * Single Responsibility: ÚNICAMENTE mapear datos entre dominio y API
 * - NO sabe de HTTP
 * - NO sabe de comunicación externa
 * - SOLO transformación de datos pura
 */
@Component
public class InalambriaCommandToPayloadMapper {

    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Mapea SendInalambriaBatchCommand a payload específico de API de Inalambria
     * Según manual oficial RESTAWS ver 3.1 - CORREGIDO según errores reales
     */
    public HashMap<String, Object> mapToApiPayload(SendInalambriaBatchCommand command) {
        HashMap<String, Object> payload = new HashMap<>();
        
        // Según documentación oficial de Inalambria
        payload.put("Type", 1); // 1 para modalidad masiva
        payload.put("MessageText", command.getMessageText());
        payload.put("Devices", mapDevicesToString(command.getDevices(), command.getCountryCode()));
        payload.put("DateMessage", ZonedDateTime.now().format(DATE_FORMAT));
        payload.put("FlashSMS", 0); // 0 o 1 como número
        payload.put("HasMore", 0); // 0 o 1 como número
        payload.put("MessagePattern", mapMessagePattern(command.getAccountType())); // Texto
        payload.put("TemplateId", 0); // Número
        payload.put("TransactionNumber", generateTransactionNumber()); // 28 dígitos
        payload.put("Url", "");
        
        return payload;
    }

    /**
     * Mapea dispositivos de Map a String (formato esperado por Inalambria)
     * Según documentación: números separados por guión
     * Soporta mensajes individuales y masivos
     * El valor del mapa es el countryCode específico para cada número
     */
    private String mapDevicesToString(Map<String, String> devices, String defaultCountryCode) {
        if (devices == null || devices.isEmpty()) {
            return "";
        }
        
        // Según documentación: números separados por guión
        StringBuilder phoneNumbers = new StringBuilder();
        
        for (Map.Entry<String, String> entry : devices.entrySet()) {
            String phoneNumber = entry.getKey();
            String countryCode = entry.getValue();
            
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                // Usar el countryCode específico del dispositivo, o el default si es null
                String effectiveCountryCode = (countryCode != null && !countryCode.isEmpty()) ? 
                    countryCode : defaultCountryCode;
                String normalizedPhone = normalizePhoneNumber(phoneNumber.trim(), effectiveCountryCode);
                
                if (phoneNumbers.length() > 0) {
                    phoneNumbers.append("-");
                }
                phoneNumbers.append(normalizedPhone);
            }
        }
        
        return phoneNumbers.toString();
    }
    
    /**
     * Normaliza el número de teléfono según documentación Inalambria
     * Formato: 57XXXXXXXXX (con prefijo país, sin +)
     */
    private String normalizePhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }
        
        phoneNumber = phoneNumber.trim();
        
        // Si tiene +57, remover el + y mantener el 57
        if (phoneNumber.startsWith("+57")) {
            return phoneNumber.substring(1);
        }
        
        // Si empieza con 57 sin +, mantenerlo
        if (phoneNumber.startsWith("57") && phoneNumber.length() >= 12) {
            return phoneNumber;
        }
        
        // Si es de 10 dígitos, agregar el código de país
        if (phoneNumber.length() == 10) {
            return (countryCode != null ? countryCode : "57") + phoneNumber;
        }
        
        return phoneNumber;
    }

    /**
     * Genera TransactionNumber de 28 dígitos según documentación
     */
    private String generateTransactionNumber() {
        // Usar timestamp actual (13 dígitos) + padding con ceros para llegar a 28
        String timestamp = String.valueOf(System.currentTimeMillis());
        String padding = "00000000000000000";
        return (timestamp + padding).substring(0, 28);
    }

    private String mapMessagePattern(SendInalambriaBatchCommand.AccountType accountType) {
        if (accountType == null) {
            return "0";
        }

        switch (accountType) {
            case TEMPLATE:
            case PERSONALIZADA:
                return "1";
            default:
                return "0";
        }
    }
}
