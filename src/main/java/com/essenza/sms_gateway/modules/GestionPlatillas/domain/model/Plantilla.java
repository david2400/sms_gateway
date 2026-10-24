package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla SMS ({@code SmsPlantilla}) con sus variables declaradas.
 * Auditoría (eliminado, usrCrea, usrMod, createdAt, updatedAt) heredada de {@link AuditInfoDto}:
 * eliminado = false ⇒ plantilla activa.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Plantilla extends AuditInfoDto {
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
    private List<PlantillaVariable> variables = new ArrayList<>();
}
