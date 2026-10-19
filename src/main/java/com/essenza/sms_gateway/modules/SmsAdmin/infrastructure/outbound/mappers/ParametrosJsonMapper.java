package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conversión de la columna {@code sms_cuenta.parametros_json} (texto JSON) ↔ mapa clave/valor.
 * La usa {@link CuentaEntityMapper} mediante {@code uses}.
 */
@Component
public class ParametrosJsonMapper {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Named("jsonToMap")
    public Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException e) {
            Map<String, Object> invalid = new LinkedHashMap<>();
            invalid.put("_jsonInvalido", json);
            return invalid;
        }
    }

    /** Solo se aceptan objetos planos: los valores no pueden ser objetos ni listas. */
    @Named("mapToJson")
    public String mapToJson(Map<String, Object> parametros) {
        if (parametros == null || parametros.isEmpty()) {
            return null;
        }
        for (Object v : parametros.values()) {
            if (v instanceof Map || v instanceof Iterable) {
                throw new AdminValidationException("parametros debe ser un objeto JSON plano (sin objetos ni listas anidadas)");
            }
        }
        try {
            return JSON.writeValueAsString(parametros);
        } catch (JsonProcessingException e) {
            throw new AdminValidationException("parametros no se pudo convertir a JSON: " + e.getOriginalMessage());
        }
    }
}
