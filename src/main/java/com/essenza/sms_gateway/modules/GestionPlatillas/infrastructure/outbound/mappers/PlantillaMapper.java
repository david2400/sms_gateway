package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaRequest;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaResponse;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaVariableRequest;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaVariableResponse;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.Plantilla;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.PlantillaVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsEntity;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsVariableEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Mapper de plantillas: API (request → modelo, modelo → respuesta) y persistencia
 * ({@code SmsPlantilla}/{@code SmsPlantillaVariable} ↔ modelo). Las relaciones (tipo, variables) las arma el adapter.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PlantillaMapper {

    // ---------- API ----------

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoMensajeId", ignore = true)
    @Mapping(target = "version", ignore = true)
    // Auditoría (AuditInfoDto): no viene en el request
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Plantilla toModel(PlantillaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orden", ignore = true)
    PlantillaVariable toModel(PlantillaVariableRequest request);

    PlantillaResponse toResponse(Plantilla plantilla);

    PlantillaVariableResponse toResponse(PlantillaVariable variable);

    // ---------- Persistencia ----------

    @Mapping(target = "id", source = "idSmsPlantilla")
    @Mapping(target = "tipoMensajeId", source = "tipoMensaje.idSmsTipoMensaje")
    @Mapping(target = "tipoMensaje", source = "tipoMensaje.codigo")
    // eliminado, usrCrea, usrMod, createdAt y updatedAt se copian por nombre (AuditInfo → AuditInfoDto)
    Plantilla toModel(PlantillaSmsEntity entity);

    @Mapping(target = "id", source = "idSmsPlantillaVariable")
    PlantillaVariable toModel(PlantillaSmsVariableEntity entity);

    List<PlantillaVariable> toVariableModels(List<PlantillaSmsVariableEntity> entities);

    /** Campos simples; id, tipo y variables los gestiona el adapter. La auditoría (AuditInfo) la gestiona JPA. */
    @Mapping(target = "idSmsPlantilla", ignore = true)
    @Mapping(target = "tipoMensaje", ignore = true)
    @Mapping(target = "variables", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(Plantilla source, @MappingTarget PlantillaSmsEntity target);

    @Mapping(target = "idSmsPlantillaVariable", ignore = true)
    @Mapping(target = "plantilla", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PlantillaSmsVariableEntity toEntity(PlantillaVariable variable);

    // ---------- Conversiones ----------

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
