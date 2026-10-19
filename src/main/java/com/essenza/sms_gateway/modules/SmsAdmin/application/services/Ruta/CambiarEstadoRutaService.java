package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.CambiarEstadoRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.RutaResponseAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class CambiarEstadoRutaService implements CambiarEstadoRutaUseCase {

    private final RutaAdminPort rutas;
    private final AdminFinder finder;
    private final RutaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public RutaResponse cambiarEstado(Long id, boolean activo) {
        Ruta saved = rutas.save(finder.ruta(id).toBuilder().activo(activo).build());
        log.info("[SMS-ADMIN] Ruta {} {}", id, activo ? "activada" : "desactivada");
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
