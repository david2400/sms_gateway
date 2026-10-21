package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;

import java.util.Optional;

/**
 * Consulta de cuentas de proveedor ({@code sms_cuenta}) fuera del motor de envío.
 * La usan los módulos de proveedor para operaciones propias que no pasan por
 * rutas ni reintentos (ej. campañas de Go4Clients).
 */
public interface ProviderAccountQueryPort {

    /**
     * Cuenta activa (cuenta y proveedor activos) por código.
     *
     * @param providerCode proveedor al que debe pertenecer la cuenta, o {@code null} para aceptar cualquier proveedor
     * @param accountCode  código de la cuenta ({@code sms_cuenta.codigo})
     */
    Optional<ProviderAccount> findActive(String providerCode, String accountCode);

    /**
     * Primera cuenta activa del proveedor (por id), para cuando el request no indica cuenta.
     */
    Optional<ProviderAccount> findFirstActive(String providerCode);
}
