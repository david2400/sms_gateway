package com.essenza.sms_gateway.modules.GestionPlatillas.application.input;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaDefinicionResponse;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaPreviewResponse;

import java.util.Map;

/**
 * Consulta y previsualización de plantillas activas (para clientes que integran el envío).
 */
public interface ConsultarPlantillaUseCase {

    /** @throws com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException si no existe */
    PlantillaDefinicionResponse describe(String code);

    /** Aplica las variables (misma validación estricta que el envío) sin enviar. */
    PlantillaPreviewResponse preview(String code, Map<String, String> variables);
}
