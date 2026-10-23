package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Definición pública de una plantilla activa: contenido y variables requeridas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plantilla SMS activa y las variables que exige")
public class PlantillaDefinicionResponse {
    @Schema(example = "RECORDATORIO_CITA")
    private String codigo;
    private String nombre;
    @Schema(description = "Tipo por defecto (null = lo define el request)", example = "NORMAL")
    private MessageType tipoMensaje;
    private int version;
    @Schema(example = "Hola {{nombre}}, su cita es el {{fecha}} a las {{hora}}.")
    private String contenido;
    private List<PlantillaVariableInfo> variables;
}
