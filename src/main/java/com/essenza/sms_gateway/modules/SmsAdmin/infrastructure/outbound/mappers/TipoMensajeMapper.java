package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper único de tipos de mensaje:
 * API (request → modelo, modelo → respuesta) y persistencia ({@code sms_tipo_mensaje} ↔ modelo).
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TipoMensajeMapper {

    // ---------- API ----------

    /** Para crear; el código se normaliza y los defaults los aplica el service. */
    @Mapping(target = "id", ignore = true)
    TipoMensaje toModel(TipoMensajeRequest request);

    /** Para actualizar; los campos null del request conservan el valor actual. El código es inmutable. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    void updateModel(TipoMensajeRequest request, @MappingTarget TipoMensaje tipo);

    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TipoMensajeResponse toResponse(TipoMensaje tipo);

    // ---------- Persistencia ----------

    @Mapping(target = "id", source = "idSmsTipoMensaje")
    TipoMensaje toModel(AdminTipoMensajeEntity entity);

    /** id lo gestiona la BD/JPA. */
    @Mapping(target = "idSmsTipoMensaje", ignore = true)
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(TipoMensaje source, @MappingTarget AdminTipoMensajeEntity target);
}
