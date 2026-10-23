package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Plantilla SMS ({@code SmsPlantilla}) con sus variables declaradas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plantilla {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    /** FK opcional a {@code SmsTipoMensaje}: tipo por defecto de los envíos con esta plantilla. */
    private Long tipoMensajeId;
    /** Código del tipo (derivado). */
    private String tipoMensaje;
    private String contenido;
    /** Se incrementa en cada modificación (queda en la respuesta de previsualización). */
    private Integer version;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private List<PlantillaVariable> variables = new ArrayList<>();
}
