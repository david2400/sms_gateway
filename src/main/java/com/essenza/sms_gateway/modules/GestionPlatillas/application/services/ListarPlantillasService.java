package com.essenza.sms_gateway.modules.GestionPlatillas.application.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.ListarPlantillasUseCase;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListarPlantillasService implements ListarPlantillasUseCase {

    private final PlantillaAdminPort plantillas;
    private final PlantillaMapper mapper;

    @Override
    public List<PlantillaResponse> listar(Boolean eliminado, String tipoMensaje) {
        String tipo = tipoMensaje == null || tipoMensaje.isBlank() ? null : tipoMensaje.trim().toUpperCase(Locale.ROOT);
        return plantillas.findAll().stream()
                .filter(p -> eliminado == null || eliminado.equals(p.getEliminado()))
                .filter(p -> tipo == null || tipo.equals(p.getTipoMensaje()))
                .map(mapper::toResponse)
                .toList();
    }
}
