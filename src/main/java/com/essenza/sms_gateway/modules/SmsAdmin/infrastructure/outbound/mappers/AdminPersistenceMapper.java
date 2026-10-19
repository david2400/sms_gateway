package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminCuentaEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminProveedorEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaPasoEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entidades JPA del módulo → modelos de dominio (MapStruct) y conversión de parametros_json.
 */
@Mapper(componentModel = "spring")
public interface AdminPersistenceMapper {

    ObjectMapper JSON = new ObjectMapper();

    Proveedor toProveedor(AdminProveedorEntity entity);

    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorCodigo", source = "proveedor.codigo")
    @Mapping(target = "parametros", source = "parametrosJson", qualifiedByName = "fromJson")
    Cuenta toCuenta(AdminCuentaEntity entity);

    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorCodigo", source = "proveedor.codigo")
    Ruta toRuta(AdminRutaEntity entity);

    @Mapping(target = "rutaId", source = "ruta.id")
    @Mapping(target = "cuentaId", source = "cuenta.id")
    @Mapping(target = "cuentaCodigo", source = "cuenta.codigo")
    @Mapping(target = "proveedorCodigo", source = "cuenta.proveedor.codigo")
    Paso toPaso(AdminRutaPasoEntity entity);

    @Named("fromJson")
    default Map<String, Object> fromJson(String json) {
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

    default String toJson(Map<String, Object> parametros) {
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
