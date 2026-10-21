package com.essenza.sms_gateway.modules.SmsCore.shared.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simétrico AES-256-GCM de las claves de las cuentas ({@code SmsCuenta.clave_cifrada}).
 * <p>
 * Formato del texto cifrado: {@code v1:} + Base64( IV de 12 bytes || texto cifrado + tag de 16 bytes ).
 * Cada cifrado usa un IV aleatorio, por eso la misma clave produce textos distintos.
 * La llave es de 32 bytes en Base64 y NUNCA se guarda en BD ni en el repositorio (variable {@code SMS_CRYPTO_KEY}).
 */
public final class AesGcmCipher {

    public static final String PREFIX = "v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    /**
     * @param base64Key llave AES de 32 bytes codificada en Base64
     * @throws IllegalArgumentException si la llave no es Base64 válido o no mide 32 bytes
     */
    public AesGcmCipher(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("La llave de cifrado está vacía");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La llave de cifrado no es Base64 válido");
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalArgumentException("La llave de cifrado debe ser de 32 bytes (AES-256); tiene " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    /** Genera una llave nueva de 32 bytes en Base64. */
    public static String generateKey() {
        byte[] raw = new byte[KEY_BYTES];
        RANDOM.nextBytes(raw);
        return Base64.getEncoder().encodeToString(raw);
    }

    public String encrypt(String plain) {
        if (plain == null) {
            throw new IllegalArgumentException("No hay texto para cifrar");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar: " + e.getClass().getSimpleName(), e);
        }
    }

    /**
     * @throws IllegalArgumentException si el texto no tiene el formato esperado o fue cifrado con otra llave
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || !cipherText.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Formato de clave cifrada no soportado (debe iniciar con " + PREFIX + ")");
        }
        try {
            byte[] data = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()).trim());
            if (data.length <= IV_BYTES) {
                throw new IllegalArgumentException("Clave cifrada incompleta");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new IllegalArgumentException("La clave no se pudo descifrar: fue cifrada con otra llave o está alterada");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar: " + e.getClass().getSimpleName(), e);
        }
    }
}
