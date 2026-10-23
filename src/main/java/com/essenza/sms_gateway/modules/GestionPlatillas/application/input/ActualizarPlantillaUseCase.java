package com.essenza.sms_gateway.modules.GestionPlatillas.application.input;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaRequest;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaResponse;

import java.util.List;

public interface ActualizarPlantillaUseCase {
    PlantillaResponse actualizar(Long id, PlantillaRequest request);
}
