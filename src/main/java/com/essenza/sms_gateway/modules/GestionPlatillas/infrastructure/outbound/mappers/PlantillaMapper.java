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
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
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
    Plantilla toModel(PlantillaSmsEntity entity);

    @Mapping(target = "id", source = "idSmsPlantillaVariable")
    PlantillaVariable toModel(PlantillaSmsVariableEntity entity);

    List<PlantillaVariable> toVariableModels(List<PlantillaSmsVariableEntity> entities);

    /** Campos simples; id, tipo, variables y fecha de creación los gestiona el adapter. */
    @Mapping(target = "idSmsPlantilla", ignore = true)
    @Mapping(target = "tipoMensaje", ignore = true)
    @Mapping(target = "variables", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    void updateEntity(Plantilla source, @MappingTarget PlantillaSmsEntity target);

    @Mapping(target = "idSmsPlantillaVariable", ignore = true)
    @Mapping(target = "plantilla", ignore = true)
    PlantillaSmsVariableEntity toEntity(PlantillaVariable variable);
}
