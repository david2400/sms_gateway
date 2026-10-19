package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/** API de proveedores: request → modelo y modelo → respuesta. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProveedorMapper {

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
    ProveedorResponse toResponse(Proveedor proveedor, boolean implementado, int cuentas);
}
