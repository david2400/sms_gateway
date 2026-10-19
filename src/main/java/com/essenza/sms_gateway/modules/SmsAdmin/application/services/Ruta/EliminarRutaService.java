package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.EliminarRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.RutaResponseAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Elimina la ruta y sus pasos. El log de envíos no se ve afectado (no referencia rutas). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class EliminarRutaService implements EliminarRutaUseCase {

    private final RutaAdminPort rutas;
    private final RutaPasoAdminPort pasos;
    private final AdminFinder finder;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public void eliminar(Long id) {
        Ruta ruta = finder.ruta(id);
        pasos.deleteByRuta(id);
        rutas.delete(id);
        log.info("[SMS-ADMIN] Ruta {} ({}/{}) eliminada", id, RutaResponseAssembler.nombre(ruta), ruta.getTipoMensaje());
        routingCache.invalidarAlConfirmar();
    }
}
