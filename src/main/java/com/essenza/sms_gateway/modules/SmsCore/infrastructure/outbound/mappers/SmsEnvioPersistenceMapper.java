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
    @Mapping(target = "fechaCreacion", source = "createdAt")
    SmsEnvioDetalleResponse toDetalle(SmsEnvioEntity entity);

    @Mapping(target = "proveedor", source = "canalProveedor.codigo")
    @Mapping(target = "cuenta", source = "canalCuenta.codigo")
    SmsEnvioDetalleResponse.Intento toIntento(SmsEnvioIntentoEntity entity);

    List<SmsEnvioDetalleResponse.Intento> toIntentos(List<SmsEnvioIntentoEntity> entities);

    /** createdAt (Instant, AuditInfo) → LocalDateTime de la respuesta. */
    default java.time.LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant == null ? null : java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
    }
}
