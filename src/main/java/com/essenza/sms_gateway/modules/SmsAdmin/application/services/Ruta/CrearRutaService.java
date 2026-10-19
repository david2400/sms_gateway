package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.CrearRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.RutaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.PasoRutaValidator;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaPasoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crea una ruta única por (proveedor, tipo); proveedor null = ruta central. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class CrearRutaService implements CrearRutaUseCase {

    private final RutaAdminPort rutas;
    private final RutaPasoAdminPort pasos;
    private final AdminFinder finder;
    private final PasoRutaValidator pasoValidator;
    private final RutaMapper rutaMapper;
    private final RutaPasoMapper pasoMapper;
    private final RutaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public RutaResponse crear(RutaRequest request) {
        if (request.getProveedorId() != null) {
            finder.proveedorReferenciado(request.getProveedorId());
        }
        Ruta ruta = rutaMapper.toModel(request);
        if (ruta.getActivo() == null) {
            ruta.setActivo(Boolean.TRUE);
        }
        rutas.findByProveedorAndTipo(ruta.getProveedorId(), ruta.getTipoMensaje()).ifPresent(r -> {
            throw new AdminConflictException("Ya existe la ruta " + RutaResponseAssembler.nombre(r) + "/" + r.getTipoMensaje()
                    + " (id " + r.getId() + "); modifique sus pasos con PUT /rutas/" + r.getId() + "/pasos");
        });
        Ruta saved = rutas.save(ruta);
        if (request.getPasos() != null && !request.getPasos().isEmpty()) {
            pasos.replace(saved.getId(), pasoValidator.validar(saved, pasoMapper.toModels(request.getPasos())));
        }
        log.info("[SMS-ADMIN] Ruta creada: {}/{} (id {})", RutaResponseAssembler.nombre(saved), saved.getTipoMensaje(), saved.getId());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
