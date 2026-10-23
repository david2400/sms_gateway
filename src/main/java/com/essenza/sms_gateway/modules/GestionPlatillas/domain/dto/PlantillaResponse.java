package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plantilla SMS")
public class PlantillaResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String tipoMensaje;
    private String contenido;
    private Integer version;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private List<PlantillaVariableResponse> variables;
}
