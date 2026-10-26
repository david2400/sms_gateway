package com.essenza.sms_gateway.modules.GestionPlatillas.application.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.ActualizarPlantillaUseCase;
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

/** Reemplaza contenido, tipo y variables. El código no cambia (lo usan los clientes y el log); la versión sube en 1. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActualizarPlantillaService implements ActualizarPlantillaUseCase {

    private final PlantillaAdminPort plantillas;
    private final PlantillaValidator validator;
    private final PlantillaMapper mapper;
    private final PlantillaCachePort cache;

    @Override
    public PlantillaResponse actualizar(Long id, PlantillaRequest request) {
        Plantilla actual = plantillas.findById(id).orElseThrow(() -> new PlantillaNoEncontradaException("No existe la plantilla " + id));
        if (!actual.getCodigo().equals(CodigosPlantilla.normalizar(request.getCodigo()))) {
            throw new PlantillaValidacionException("El código de la plantilla no se puede cambiar (" + actual.getCodigo()
                    + "); cree una plantilla nueva");
        }
        Plantilla p = validator.validar(mapper.toModel(request));
        p.setId(actual.getId());
        p.setEliminado(actual.getEliminado());
        p.setVersion((actual.getVersion() == null ? 1 : actual.getVersion()) + 1);
        p.setFechaModificacion(LocalDateTime.now());
        Plantilla saved = plantillas.save(p);
        log.info("[SMS-ADMIN] Plantilla {} actualizada a la versión {}", saved.getCodigo(), saved.getVersion());
        cache.invalidarAlConfirmar();
        return mapper.toResponse(saved);
    }
}
