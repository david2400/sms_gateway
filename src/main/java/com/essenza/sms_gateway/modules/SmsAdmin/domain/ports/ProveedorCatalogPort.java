package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

import java.util.Set;

/** Proveedores que tienen módulo implementado en el código (SmsSenderPort registrados). */
public interface ProveedorCatalogPort {
    Set<String> implementados();

    default boolean isImplementado(String codigo) {
        return codigo != null && implementados().contains(codigo.toUpperCase());
    }
}
