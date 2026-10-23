package com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TipoMensajeRef;

import java.util.Optional;

/** Consulta del catálogo {@code SmsTipoMensaje} (solo lectura). */
public interface TipoMensajeConsultaPort {
    /** Código normalizado (mayúsculas). */
    Optional<TipoMensajeRef> findByCodigo(String codigo);
}
