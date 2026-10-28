package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
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
public class PlantillaResponse extends AuditInfoDto {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String tipoMensaje;
    private String contenido;
    private Integer version;
    private Boolean eliminado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private List<PlantillaVariableResponse> variables;
}
