package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Paso de una ruta ({@code sms_ruta_paso}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paso {
    private Long id;
    private Long rutaId;
    private Integer orden;
    private Long cuentaId;
    private String cuentaCodigo;
    private String proveedorCodigo;
    private Integer maxIntentos;
    private Integer esperaMs;
    private Boolean eliminado;
}
