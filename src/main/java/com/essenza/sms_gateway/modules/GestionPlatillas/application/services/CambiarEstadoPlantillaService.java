package com.essenza.sms_gateway.modules.GestionPlatillas.application.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.CambiarEstadoPlantillaUseCase;
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

/** Desactivada, los envíos con esa plantilla responden 422 PLANTILLA_NO_ENCONTRADA. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CambiarEstadoPlantillaService implements CambiarEstadoPlantillaUseCase {

    private final PlantillaAdminPort plantillas;
    private final PlantillaMapper mapper;
    private final PlantillaCachePort cache;

    @Override
    public PlantillaResponse cambiarEstado(Long id, boolean eliminado) {
        Plantilla p = plantillas.findById(id).orElseThrow(() -> new PlantillaNoEncontradaException("No existe la plantilla " + id));
        p.setEliminado(eliminado);
        p.setFechaModificacion(LocalDateTime.now());
        Plantilla saved = plantillas.save(p);
        log.info("[SMS-ADMIN] Plantilla {} {}", saved.getCodigo(), eliminado ? "desactivada" : "activada");
        cache.invalidarAlConfirmar();
        return mapper.toResponse(saved);
    }
}
