package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.VariableDataType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Variable declarada de una plantilla ({@code SmsPlantillaVariable}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaVariable {
    private Long id;
    private String nombre;
    private VariableDataType tipoDato;
    private Boolean obligatoria;
    private Integer longitudMax;
    private String patron;
    private String valorDefecto;
    private String descripcion;
    private Integer orden;
}
