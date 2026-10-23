package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;

import java.util.List;
import java.util.Set;

/** Persistencia de {@code SmsProveedorServicio}: servicios (tipos de mensaje) habilitados por proveedor. */
public interface ProveedorServicioAdminPort {

    /** Tipos habilitados del proveedor, ordenados por código. */
    List<TipoMensaje> tiposDe(Long proveedorId);

    boolean soporta(Long proveedorId, Long tipoMensajeId);

    /** Reemplaza la lista completa de servicios del proveedor. */
    void reemplazar(Long proveedorId, Set<Long> tipoMensajeIds);

    /** Rutas (descripción legible) que usan cuentas del proveedor para el tipo. */
    List<String> rutasQueUsan(Long proveedorId, Long tipoMensajeId);
}
