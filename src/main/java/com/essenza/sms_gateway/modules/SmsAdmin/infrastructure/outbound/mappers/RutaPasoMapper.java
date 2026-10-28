package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.PasoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.SmsRutaPasoEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper único de pasos de ruta:
 * API (request → modelo, modelo → respuesta) y persistencia ({@code sms_ruta_paso} ↔ modelo).
 * Ruta y cuenta de la entidad las asigna el adapter (getReferenceById); los defaults los aplica PasoRutaValidator.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RutaPasoMapper {

    // ---------- API ----------

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rutaId", ignore = true)
    @Mapping(target = "cuentaCodigo", ignore = true)
    @Mapping(target = "proveedorCodigo", ignore = true)
    Paso toModel(PasoRequest request);

    List<Paso> toModels(List<PasoRequest> requests);

    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RutaResponse.Paso toResponse(Paso paso);

    List<RutaResponse.Paso> toResponses(List<Paso> pasos);

    // ---------- Persistencia ----------

    @Mapping(target = "id", source = "idSmsRutaPaso")
    @Mapping(target = "rutaId", source = "ruta.idSmsRuta")
    @Mapping(target = "cuentaId", source = "cuenta.idSmsCuenta")
    @Mapping(target = "cuentaCodigo", source = "cuenta.codigo")
    @Mapping(target = "proveedorCodigo", source = "cuenta.proveedor.codigo")
    Paso toModel(SmsRutaPasoEntity entity);

    @Mapping(target = "idSmsRutaPaso", ignore = true)
    @Mapping(target = "ruta", ignore = true)
    @Mapping(target = "cuenta", ignore = true)
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SmsRutaPasoEntity toEntity(Paso paso);
}
