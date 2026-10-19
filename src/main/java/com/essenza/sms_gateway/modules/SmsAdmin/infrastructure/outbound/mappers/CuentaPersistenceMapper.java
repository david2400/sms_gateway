package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminCuentaEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/** {@code sms_cuenta}: entidad ↔ modelo. El proveedor lo asigna el adapter (getReferenceById). */
@Mapper(componentModel = "spring", uses = ParametrosJsonMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CuentaPersistenceMapper {

    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorCodigo", source = "proveedor.codigo")
    @Mapping(target = "parametros", source = "parametrosJson", qualifiedByName = "jsonToMap")
    Cuenta toModel(AdminCuentaEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "parametrosJson", source = "parametros", qualifiedByName = "mapToJson")
    void updateEntity(Cuenta source, @MappingTarget AdminCuentaEntity target);
}
