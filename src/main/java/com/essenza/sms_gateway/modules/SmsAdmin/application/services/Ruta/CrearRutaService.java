package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.CrearRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.PasoRutaValidator;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.RutaResponseAssembler;
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
    private final RutaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public RutaResponse crear(RutaRequest request) {
        Long proveedorId = request.getProveedorId();
        if (proveedorId != null) {
            finder.proveedorReferenciado(proveedorId);
        }
        String tipo = request.getTipoMensaje().name();
        rutas.findByProveedorAndTipo(proveedorId, tipo).ifPresent(r -> {
            throw new AdminConflictException("Ya existe la ruta " + RutaResponseAssembler.nombre(r) + "/" + tipo
                    + " (id " + r.getId() + "); modifique sus pasos con PUT /rutas/" + r.getId() + "/pasos");
        });
        Ruta ruta = rutas.save(Ruta.builder()
                .proveedorId(proveedorId)
                .tipoMensaje(tipo)
                .activo(request.getActivo() == null || request.getActivo())
                .build());
        if (request.getPasos() != null && !request.getPasos().isEmpty()) {
            pasos.replace(ruta.getId(), pasoValidator.validar(ruta, request.getPasos()));
        }
        log.info("[SMS-ADMIN] Ruta creada: {}/{} (id {})", RutaResponseAssembler.nombre(ruta), tipo, ruta.getId());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(ruta);
    }
}
