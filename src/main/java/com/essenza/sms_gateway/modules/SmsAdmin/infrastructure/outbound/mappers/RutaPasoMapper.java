package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.PasoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/** Pasos de ruta: request → modelo (los defaults los aplica PasoRutaValidator) y modelo → respuesta. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RutaPasoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rutaId", ignore = true)
    @Mapping(target = "cuentaCodigo", ignore = true)
    @Mapping(target = "proveedorCodigo", ignore = true)
    Paso toModel(PasoRequest request);

    List<Paso> toModels(List<PasoRequest> requests);

    RutaResponse.Paso toResponse(Paso paso);

    List<RutaResponse.Paso> toResponses(List<Paso> pasos);
}
