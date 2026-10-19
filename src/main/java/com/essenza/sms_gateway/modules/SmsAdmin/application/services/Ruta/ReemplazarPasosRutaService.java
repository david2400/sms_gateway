package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.ReemplazarPasosRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.RutaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.PasoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.PasoRutaValidator;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaPasoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reemplaza la lista completa de pasos (permite reordenar, agregar y quitar en una sola operación). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class ReemplazarPasosRutaService implements ReemplazarPasosRutaUseCase {

    private final RutaPasoAdminPort pasos;
    private final AdminFinder finder;
    private final PasoRutaValidator pasoValidator;
    private final RutaPasoMapper pasoMapper;
    private final RutaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public RutaResponse reemplazarPasos(Long id, List<PasoRequest> nuevos) {
        Ruta ruta = finder.ruta(id);
        pasos.replace(id, pasoValidator.validar(ruta, pasoMapper.toModels(nuevos)));
        log.info("[SMS-ADMIN] Ruta {} ({}/{}): {} pasos", id, RutaResponseAssembler.nombre(ruta), ruta.getTipoMensaje(), nuevos.size());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(ruta);
    }
}
