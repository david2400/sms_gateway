package com.essenza.sms_gateway.modules.SmsCore.domain.model;

/**
 * Canal por el que entró el envío.
 * <ul>
 *   <li>{@link #CENTRAL}: endpoint centralizado, la ruta puede mezclar proveedores.</li>
 *   <li>{@link #PROVEEDOR}: endpoint propio de un proveedor, solo usa cuentas de ese proveedor.</li>
 * </ul>
 */
public enum DispatchChannel {
    CENTRAL,
    PROVEEDOR
}
