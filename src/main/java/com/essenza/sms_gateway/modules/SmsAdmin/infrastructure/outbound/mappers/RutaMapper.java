package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper único de rutas:
 * API (request → modelo, ruta + pasos → respuesta) y persistencia ({@code sms_ruta} ↔ modelo).
 * El proveedor de la entidad lo asigna el adapter (null = ruta central) y los pasos los convierte {@link RutaPasoMapper}.
 */
@Mapper(componentModel = "spring", uses = RutaPasoMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RutaMapper {

    // ---------- API ----------

    /** Los pasos del request se convierten aparte con {@link RutaPasoMapper#toModels};
     *  el código del tipo lo resuelve el service desde {@code tipoMensajeId}. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proveedorCodigo", ignore = true)
    @Mapping(target = "tipoMensaje", ignore = true)
    Ruta toModel(RutaRequest request);

    /** {@code nombre} = "CENTRAL" o el código del proveedor. */
    @Mapping(target = "id", source = "ruta.id")
    @Mapping(target = "proveedorId", source = "ruta.proveedorId")
    @Mapping(target = "proveedorCodigo", source = "nombre")
    @Mapping(target = "tipoMensajeId", source = "ruta.tipoMensajeId")
    @Mapping(target = "tipoMensaje", source = "ruta.tipoMensaje")
    @Mapping(target = "activo", source = "ruta.activo")
    @Mapping(target = "pasos", source = "pasos")
    RutaResponse toResponse(Ruta ruta, String nombre, List<Paso> pasos);

    // ---------- Persistencia ----------

    @Mapping(target = "id", source = "idSmsRuta")
    @Mapping(target = "proveedorId", source = "proveedor.idSmsProveedor")
    @Mapping(target = "proveedorCodigo", source = "proveedor.codigo")
    @Mapping(target = "tipoMensajeId", source = "tipoMensaje.idSmsTipoMensaje")
    @Mapping(target = "tipoMensaje", source = "tipoMensaje.codigo")
    Ruta toModel(AdminRutaEntity entity);

    @Mapping(target = "idSmsRuta", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "tipoMensaje", ignore = true)
    void updateEntity(Ruta source, @MappingTarget AdminRutaEntity target);
}
