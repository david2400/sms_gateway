package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/** Proveedor de SMS ({@code sms_proveedor}). */
@Value
@Builder(toBuilder = true)
public class Proveedor {
    Long id;
    String codigo;
    String nombre;
    String baseUrl;
    Integer connectTimeoutMs;
    Integer readTimeoutMs;
    boolean activo;
    LocalDateTime fechaCreacion;
}
