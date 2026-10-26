package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Proveedor de SMS ({@code sms_proveedor}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {
    private Long id;
    private String codigo;
    private String nombre;
    private String baseUrl;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private Boolean eliminado;
}
