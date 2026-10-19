package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;

import java.util.List;
import java.util.Optional;

/** Persistencia de {@code sms_ruta}. */
public interface RutaAdminPort {
    /**
     * @param proveedorId filtro opcional
     * @param soloCentral true = solo rutas del endpoint central (proveedor NULL)
     * @param tipoMensaje filtro opcional
     */
    List<Ruta> findAll(Long proveedorId, boolean soloCentral, String tipoMensaje);
    Optional<Ruta> findById(Long id);
    /** @param proveedorId null = ruta central */
    Optional<Ruta> findByProveedorAndTipo(Long proveedorId, String tipoMensaje);
    Ruta save(Ruta ruta);
    /** Borra la ruta (los pasos deben borrarse antes con {@link RutaPasoAdminPort#deleteByRuta}). */
    void delete(Long rutaId);
}
