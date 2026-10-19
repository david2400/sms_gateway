package com.essenza.sms_gateway.modules.SmsAdmin.application.services.support;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorCatalogPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.ProveedorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Respuesta de proveedor: datos del modelo + si tiene módulo implementado + cantidad de cuentas. */
@Component
@RequiredArgsConstructor
public class ProveedorResponseAssembler {

    private final ProveedorMapper mapper;
    private final ProveedorCatalogPort catalog;
    private final CuentaAdminPort cuentas;

    public ProveedorResponse toResponse(Proveedor proveedor) {
        return mapper.toResponse(proveedor,
                catalog.isImplementado(proveedor.getCodigo()),
                cuentas.findAll(proveedor.getId()).size());
    }
}
