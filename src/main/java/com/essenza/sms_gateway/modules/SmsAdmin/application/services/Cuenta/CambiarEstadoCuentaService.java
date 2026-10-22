package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.CambiarEstadoCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.CuentaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Desactivada, el motor salta la cuenta en todas las rutas. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CambiarEstadoCuentaService implements CambiarEstadoCuentaUseCase {

    private final CuentaAdminPort cuentas;
    private final AdminFinder finder;
    private final CuentaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public CuentaResponse cambiarEstado(Long id, boolean eliminado) {
        Cuenta cuenta = finder.cuenta(id);
        cuenta.setEliminado(eliminado);
        Cuenta saved = cuentas.save(cuenta);
        log.info("[SMS-ADMIN] Cuenta {} {}", saved.getCodigo(), eliminado ? "desactivada" : "activada");
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
