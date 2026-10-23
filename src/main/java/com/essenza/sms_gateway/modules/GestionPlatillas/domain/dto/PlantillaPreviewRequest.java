package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Variables para previsualizar una plantilla sin enviar. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaPreviewRequest {
    @Size(max = 50)
    @Schema(example = "{\"nombre\": \"Ana\", \"fecha\": \"2026-11-05\", \"hora\": \"14:30\"}")
    private Map<String, String> variables;
}
