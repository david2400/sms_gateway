package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;

import java.util.Map;

/**
 * Cuenta de un proveedor ({@code sms_cuenta} + datos de {@code sms_proveedor}),
 * con la contraseña ya descifrada ({@code clave_cifrada}) o resuelta desde su {@code secret_ref}.
 */
@Value
@Builder
public class ProviderAccount {

    Long id;
    String code;

    Long providerId;
    /** Código del proveedor (= {@code SmsSenderPort.providerCode()}). */
    String providerCode;
    String baseUrl;
    int connectTimeoutMs;
    int readTimeoutMs;

    String username;

    @ToString.Exclude
    String password;

    String sender;

    /** Parámetros propios del proveedor ({@code sms_cuenta.parametros_json}). */
    @Singular
    Map<String, String> parameters;

    public String parameter(String key) {
        return parameters != null ? parameters.get(key) : null;
    }

    public boolean hasCredentials() {
        return password != null && !password.isBlank();
    }
}
