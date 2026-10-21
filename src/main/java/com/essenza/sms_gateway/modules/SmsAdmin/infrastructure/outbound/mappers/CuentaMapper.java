package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/** API de cuentas: request → modelo y modelo → respuesta (nunca incluye contraseñas). */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CuentaMapper {

    /** Para crear; los valores por defecto los aplica el service. */
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
    CuentaResponse toResponse(Cuenta cuenta, boolean secretDefinido, List<Long> rutas);
}
