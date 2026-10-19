package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaEfectivaResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** Ruta efectiva: paso del motor de SmsCore ({@link RouteStep}) → respuesta. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RutaEfectivaMapper {

    @Mapping(target = "orden", source = "step.order")
    @Mapping(target = "proveedor", source = "step.account.providerCode")
    @Mapping(target = "cuenta", source = "step.account.code")
    @Mapping(target = "maxIntentos", source = "step.maxAttempts")
    @Mapping(target = "esperaMs", source = "step.waitMs")
    @Mapping(target = "credencialesOk", source = "credencialesOk")
    @Mapping(target = "implementado", source = "implementado")
    RutaEfectivaResponse.Paso toPaso(RouteStep step, boolean credencialesOk, boolean implementado);
}
