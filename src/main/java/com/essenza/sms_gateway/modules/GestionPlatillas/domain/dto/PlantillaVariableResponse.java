package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.VariableDataType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaVariableResponse {
    private String nombre;
    private VariableDataType tipoDato;
    private Boolean obligatoria;
    private Integer longitudMax;
    private String patron;
    private String valorDefecto;
    private String descripcion;
    private Integer orden;
}
