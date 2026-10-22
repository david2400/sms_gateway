package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.CambiarEstadoProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.ProveedorResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Desactivado, el motor salta todas las cuentas del proveedor. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CambiarEstadoProveedorService implements CambiarEstadoProveedorUseCase {

    private final ProveedorAdminPort proveedores;
    private final AdminFinder finder;
    private final ProveedorResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public ProveedorResponse cambiarEstado(Long id, boolean eliminado) {
        Proveedor proveedor = finder.proveedor(id);
        proveedor.setEliminado(eliminado);
        Proveedor saved = proveedores.save(proveedor);
        log.info("[SMS-ADMIN] Proveedor {} {}", saved.getCodigo(), eliminado ? "desactivado" : "activado");
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
