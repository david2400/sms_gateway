package com.essenza.sms_gateway.modules.SmsAdmin.domain.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Arma la respuesta de proveedor (incluye si tiene módulo implementado y cuántas cuentas tiene). */
@Service
@RequiredArgsConstructor
public class ProveedorResponseAssembler {

    private final ProveedorCatalogPort catalog;
    private final CuentaAdminPort cuentas;

    public ProveedorResponse toResponse(Proveedor p) {
        return ProveedorResponse.builder()
                .id(p.getId())
                .codigo(p.getCodigo())
                .nombre(p.getNombre())
                .baseUrl(p.getBaseUrl())
                .connectTimeoutMs(p.getConnectTimeoutMs())
                .readTimeoutMs(p.getReadTimeoutMs())
                .activo(p.isActivo())
                .fechaCreacion(p.getFechaCreacion())
                .implementado(catalog.isImplementado(p.getCodigo()))
                .cuentas(cuentas.findAll(p.getId()).size())
                .build();
    }
}
