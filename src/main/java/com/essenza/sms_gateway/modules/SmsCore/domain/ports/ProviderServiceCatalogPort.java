package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;

import java.util.Set;

/**
 * Servicios (tipos de mensaje) que tiene habilitados cada proveedor ({@code SmsProveedorServicio}).
 * <p>
 * Es la fuente de verdad para decidir si un proveedor puede atender un tipo de mensaje: el motor la usa
 * para rechazar envíos a proveedores sin el servicio y para que el fallback solo pase por proveedores
 * que soportan el mismo tipo de envío.
 */
public interface ProviderServiceCatalogPort {

    /**
     * @param providerCode {@code sms_proveedor.codigo} (sin importar mayúsculas)
     * @param type         tipo de mensaje
     * @return {@code true} si el proveedor está activo y tiene el tipo habilitado
     */
    boolean supports(String providerCode, MessageType type);

    /** Tipos habilitados del proveedor (vacío si no existe o no tiene ninguno). */
    Set<MessageType> servicesOf(String providerCode);

    /** Descarta la caché para tomar cambios hechos en BD. */
    void refresh();
}
