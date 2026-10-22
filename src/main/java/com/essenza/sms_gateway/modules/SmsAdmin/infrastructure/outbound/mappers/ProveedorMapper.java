package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminProveedorEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper único de proveedores:
 * API (request → modelo, modelo → respuesta) y persistencia ({@code sms_proveedor} ↔ modelo).
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProveedorMapper {

    // ---------- API ----------

    /** Para crear; los valores por defecto los aplica el service. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Proveedor toModel(ProveedorRequest request);

    /** Para actualizar; los campos null del request conservan el valor actual. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    void updateModel(ProveedorRequest request, @MappingTarget Proveedor proveedor);

    @Mapping(target = "implementado", source = "implementado")
    @Mapping(target = "cuentas", source = "cuentas")
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProveedorResponse toResponse(Proveedor proveedor, boolean implementado, int cuentas);

    // ---------- Persistencia ----------

    @Mapping(target = "id", source = "idSmsProveedor")
    Proveedor toModel(AdminProveedorEntity entity);

    /** id y fecha de creación los gestiona la BD/JPA. */
    @Mapping(target = "idSmsProveedor", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "usrCrea", ignore = true)
    @Mapping(target = "usrMod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(Proveedor source, @MappingTarget AdminProveedorEntity target);
}
