package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/** API de rutas: request → modelo y ruta + pasos → respuesta. Los pasos los convierte {@link RutaPasoMapper}. */
@Mapper(componentModel = "spring", uses = RutaPasoMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RutaMapper {

    /** Los pasos del request se convierten aparte con {@link RutaPasoMapper#toModels}. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proveedorCodigo", ignore = true)
    Ruta toModel(RutaRequest request);

    /** {@code nombre} = "CENTRAL" o el código del proveedor. */
    @Mapping(target = "id", source = "ruta.id")
    @Mapping(target = "proveedorId", source = "ruta.proveedorId")
    @Mapping(target = "proveedorCodigo", source = "nombre")
    @Mapping(target = "tipoMensaje", source = "ruta.tipoMensaje")
    @Mapping(target = "activo", source = "ruta.activo")
    @Mapping(target = "pasos", source = "pasos")
    RutaResponse toResponse(Ruta ruta, String nombre, List<Paso> pasos);
}
