package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminCuentaEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper único de cuentas:
 * API (request → modelo, modelo → respuesta) y persistencia ({@code sms_cuenta} ↔ modelo).
 * El proveedor de la entidad lo asigna el adapter (getReferenceById) y las contraseñas nunca salen en la respuesta.
 */
@Mapper(componentModel = "spring", uses = ParametrosJsonMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CuentaMapper {

    // ---------- API ----------

    /** Para crear; los valores por defecto los aplica el service (la clave la cifra el service). */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proveedorCodigo", ignore = true)
    @Mapping(target = "claveCifrada", ignore = true)
    Cuenta toModel(CuentaRequest request);

    /** Para actualizar; los campos null del request conservan el valor actual. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proveedorCodigo", ignore = true)
    @Mapping(target = "claveCifrada", ignore = true)
    void updateModel(CuentaRequest request, @MappingTarget Cuenta cuenta);

    @Mapping(target = "secretDefinido", source = "secretDefinido")
    @Mapping(target = "rutas", source = "rutas")
    @Mapping(target = "claveConfigurada", expression = "java(cuenta.getClaveCifrada() != null && !cuenta.getClaveCifrada().isBlank())")
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CuentaResponse toResponse(Cuenta cuenta, boolean secretDefinido, List<Long> rutas);

    // ---------- Persistencia ----------

    @Mapping(target = "id", source = "idSmsCuenta")
    @Mapping(target = "proveedorId", source = "proveedor.idSmsProveedor")
    @Mapping(target = "proveedorCodigo", source = "proveedor.codigo")
    @Mapping(target = "parametros", source = "parametrosJson", qualifiedByName = "jsonToMap")
    Cuenta toModel(AdminCuentaEntity entity);

    @Mapping(target = "idSmsCuenta", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "parametrosJson", source = "parametros", qualifiedByName = "mapToJson")
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(Cuenta source, @MappingTarget AdminCuentaEntity target);
}
