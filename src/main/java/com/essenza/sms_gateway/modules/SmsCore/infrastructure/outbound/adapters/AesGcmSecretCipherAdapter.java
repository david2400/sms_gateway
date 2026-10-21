package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SecretCipherPort;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.config.SmsCoreProperties;
import com.essenza.sms_gateway.modules.SmsCore.shared.crypto.AesGcmCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link SecretCipherPort} con AES-256-GCM. La llave sale de {@code sms.core.crypto-key}
 * ({@code SMS_CRYPTO_KEY}); si falta o es inválida la aplicación arranca igual, pero las cuentas con
 * {@code clave_cifrada} quedan sin credenciales y SmsAdmin no permite guardar claves.
 */
@Slf4j
@Component
public class AesGcmSecretCipherAdapter implements SecretCipherPort {

    private final AesGcmCipher cipher;

    public AesGcmSecretCipherAdapter(SmsCoreProperties properties) {
        AesGcmCipher built = null;
        String key = properties.getCryptoKey();
        if (key == null || key.isBlank()) {
            log.warn("[SMS-CORE] SMS_CRYPTO_KEY no está definida: las claves cifradas de las cuentas no se podrán usar");
        } else {
            try {
                built = new AesGcmCipher(key);
            } catch (IllegalArgumentException e) {
                log.error("[SMS-CORE] SMS_CRYPTO_KEY inválida: {}", e.getMessage());
            }
        }
        this.cipher = built;
    }

    @Override
    public boolean isConfigured() {
        return cipher != null;
    }

    @Override
    public String encrypt(String plain) {
        return requireCipher().encrypt(plain);
    }

    @Override
    public String decrypt(String cipherText) {
        return requireCipher().decrypt(cipherText);
    }

    private AesGcmCipher requireCipher() {
        if (cipher == null) {
            throw new IllegalStateException("No hay llave de cifrado configurada (SMS_CRYPTO_KEY)");
        }
        return cipher;
    }
}
