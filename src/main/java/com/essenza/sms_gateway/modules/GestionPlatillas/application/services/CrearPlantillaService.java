package com.essenza.sms_gateway.modules.GestionPlatillas.application.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.CrearPlantillaUseCase;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaRequest;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaResponse;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception.PlantillaConflictoException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception.PlantillaNoEncontradaException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception.PlantillaValidacionException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.Plantilla;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaAdminPort;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaCachePort;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.services.PlantillaValidator;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.mappers.PlantillaMapper;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.services.CodigosPlantilla;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Crea una plantilla (versión 1, activa). El motor la ve después del commit (se invalida su caché). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CrearPlantillaService implements CrearPlantillaUseCase {

    private final PlantillaAdminPort plantillas;
    private final PlantillaValidator validator;
    private final PlantillaMapper mapper;
    private final PlantillaCachePort cache;

    @Override
    public PlantillaResponse crear(PlantillaRequest request) {
        Plantilla p = validator.validar(mapper.toModel(request));
        plantillas.findByCodigo(p.getCodigo()).ifPresent(x -> {
            throw new PlantillaConflictoException("Ya existe la plantilla " + p.getCodigo() + " (id " + x.getId() + ")");
        });
        p.setVersion(1);
        p.setActivo(Boolean.TRUE);
        p.setFechaCreacion(LocalDateTime.now());
        Plantilla saved = plantillas.save(p);
        log.info("[SMS-ADMIN] Plantilla creada: {} (id {}, {} variables)", saved.getCodigo(), saved.getId(), saved.getVariables().size());
        cache.invalidarAlConfirmar();
        return mapper.toResponse(saved);
    }
}
