package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.ConsultarPlantillaUseCase;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaDefinicionResponse;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaPreviewRequest;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Plantillas SMS para clientes del API: qué variables pide cada una y cómo queda el texto.
 * La administración (crear/editar) está en {@code /v1/sms/admin/plantillas}.
 */
@RestController
@RequestMapping("/v1/sms/plantillas")
@RequiredArgsConstructor
@Tag(name = "SMS Plantillas", description = "Consulta y previsualización de plantillas activas")
public class PlantillaController {

    private final ConsultarPlantillaUseCase templateUseCase;

    @Operation(summary = "Definición de una plantilla", description = "Contenido y variables que se deben enviar. 422 si no existe o está inactiva.")
    @GetMapping("/{codigo}")
    public PlantillaDefinicionResponse describe(@PathVariable String codigo) {
        return templateUseCase.describe(codigo);
    }

    @Operation(summary = "Previsualizar plantilla",
            description = "Aplica las variables con la misma validación estricta del envío y devuelve el texto, la codificación "
                    + "y las partes SMS. No envía ni registra nada. 400 con la lista de errores por variable.")
    @PostMapping("/{codigo}/previsualizar")
    public PlantillaPreviewResponse preview(@PathVariable String codigo, @Valid @RequestBody PlantillaPreviewRequest request) {
        return templateUseCase.preview(codigo, request.getVariables());
    }
}
