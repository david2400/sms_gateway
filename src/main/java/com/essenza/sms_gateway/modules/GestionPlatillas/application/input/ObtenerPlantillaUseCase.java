package com.essenza.sms_gateway.modules.GestionPlatillas.application.input;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaRequest;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaResponse;

import java.util.List;

public interface ObtenerPlantillaUseCase {
    PlantillaResponse obtener(Long id);
    PlantillaResponse obtenerPorCodigo(String codigo);
}
