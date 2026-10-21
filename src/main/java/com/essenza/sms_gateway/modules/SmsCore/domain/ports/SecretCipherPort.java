package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

/**
 * Cifra y descifra las claves de las cuentas de proveedor ({@code SmsCuenta.clave_cifrada}).
 * La llave vive fuera de la BD (variable de entorno {@code SMS_CRYPTO_KEY}).
 */
public interface SecretCipherPort {

    /** {@code true} si hay una llave válida configurada. */
    boolean isConfigured();

    /** @throws IllegalStateException si no hay llave configurada */
    String encrypt(String plain);

    /**
     * @throws IllegalStateException    si no hay llave configurada
     * @throws IllegalArgumentException si el texto fue cifrado con otra llave o está alterado
     */
    String decrypt(String cipherText);
}
