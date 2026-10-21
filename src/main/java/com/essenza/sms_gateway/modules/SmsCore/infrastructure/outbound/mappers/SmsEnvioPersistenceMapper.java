package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioDetalleResponse;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsEnvioEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsEnvioIntentoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Entidades del log → DTOs de consulta (MapStruct).
 */
@Mapper(componentModel = "spring")
public interface SmsEnvioPersistenceMapper {

    @Mapping(target = "envioId", source = "uuid")
    @Mapping(target = "proveedorFinal", source = "proveedorFinal.codigo")
    @Mapping(target = "cuentaFinal", source = "cuentaFinal.codigo")
    @Mapping(target = "intentos", ignore = true)
    SmsEnvioDetalleResponse toDetalle(SmsEnvioEntity entity);

    @Mapping(target = "proveedor", source = "proveedor.codigo")
    @Mapping(target = "cuenta", source = "cuenta.codigo")
    SmsEnvioDetalleResponse.Intento toIntento(SmsEnvioIntentoEntity entity);

    List<SmsEnvioDetalleResponse.Intento> toIntentos(List<SmsEnvioIntentoEntity> entities);
}
