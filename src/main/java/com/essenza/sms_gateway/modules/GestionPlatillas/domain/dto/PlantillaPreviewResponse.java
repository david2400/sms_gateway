package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.SmsEncoding;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Texto final de una plantilla con las variables dadas y su costo en partes SMS. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Previsualización (no envía ni registra nada)")
public class PlantillaPreviewResponse {
    private String plantilla;
    private int version;
    private MessageType tipoMensaje;
    @Schema(example = "Hola Ana, su cita es el 05/11/2026 a las 2:30 PM.")
    private String texto;
    @Schema(description = "Longitud en unidades de la codificación", example = "49")
    private int longitud;
    @Schema(description = "GSM7 (160 por SMS) o UCS2 (70 por SMS; ej. por tildes á, í, ó, ú)", example = "GSM7")
    private SmsEncoding codificacion;
    @Schema(description = "SMS facturables que ocupa", example = "1")
    private int partes;
}
