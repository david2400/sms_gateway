package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

/**
 * Tipo de mensaje ({@code SmsTipoMensaje}) visto por el módulo de plantillas: solo lo necesario para
 * asociarlo como tipo por defecto de una plantilla.
 */
public record TipoMensajeRef(Long id, String codigo, boolean eliminado) {
}
