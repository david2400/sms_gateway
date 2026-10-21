package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminProveedorEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/** {@code sms_proveedor}: entidad ↔ modelo. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProveedorEntityMapper {

    @Mapping(target = "id", source = "idSmsProveedor")
    Proveedor toModel(AdminProveedorEntity entity);

    /** id y fecha de creación los gestiona la BD/JPA. */
    @Mapping(target = "idSmsProveedor", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    void updateEntity(Proveedor source, @MappingTarget AdminProveedorEntity target);
}
