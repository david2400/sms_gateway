package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaPasoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** {@code sms_ruta_paso}: entidad ↔ modelo. Ruta y cuenta las asigna el adapter (getReferenceById). */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RutaPasoPersistenceMapper {

    @Mapping(target = "rutaId", source = "ruta.id")
    @Mapping(target = "cuentaId", source = "cuenta.id")
    @Mapping(target = "cuentaCodigo", source = "cuenta.codigo")
    @Mapping(target = "proveedorCodigo", source = "cuenta.proveedor.codigo")
    Paso toModel(AdminRutaPasoEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ruta", ignore = true)
    @Mapping(target = "cuenta", ignore = true)
    AdminRutaPasoEntity toEntity(Paso paso);
}
