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
     * Cuenta activa por código, validando que pertenezca al proveedor indicado.
     */
    Optional<ProviderAccount> findActive(String providerCode, String accountCode);

    /**
     * Primera cuenta activa del proveedor (por id), para cuando el request no indica cuenta.
     */
    Optional<ProviderAccount> findFirstActive(String providerCode);
}
