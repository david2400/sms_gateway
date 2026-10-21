package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/** {@code sms_ruta}: entidad ↔ modelo. El proveedor lo asigna el adapter (null = ruta central). */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RutaEntityMapper {

    @Mapping(target = "id", source = "idSmsRuta")
    @Mapping(target = "proveedorId", source = "proveedor.idSmsProveedor")
    @Mapping(target = "proveedorCodigo", source = "proveedor.codigo")
    Ruta toModel(AdminRutaEntity entity);

    @Mapping(target = "idSmsRuta", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    void updateEntity(Ruta source, @MappingTarget AdminRutaEntity target);
}
