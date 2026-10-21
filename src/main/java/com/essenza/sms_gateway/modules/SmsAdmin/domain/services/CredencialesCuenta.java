package com.essenza.sms_gateway.modules.SmsAdmin.domain.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SecretCipherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Credenciales de una cuenta: cifra la clave recibida en el request y exige que la cuenta quede con
 * clave cifrada o con {@code secret_ref}. La clave en texto plano nunca se guarda ni se registra.
 */
@Service
@RequiredArgsConstructor
public class CredencialesCuenta {

    private final SecretCipherPort cipher;

    /**
     * @param cuenta     cuenta ya mapeada desde el request (en una actualización conserva su clave actual)
     * @param clavePlana clave del request; {@code null} o vacía = no cambia
     */
    public void aplicar(Cuenta cuenta, String clavePlana) {
        if (clavePlana != null && !clavePlana.isBlank()) {
            if (!cipher.isConfigured()) {
                throw new AdminValidationException("No se puede guardar la clave: el servidor no tiene la llave de cifrado (SMS_CRYPTO_KEY)");
            }
            cuenta.setClaveCifrada(cipher.encrypt(clavePlana));
        }
        cuenta.setSecretRef(AdminCodigos.blankToNull(cuenta.getSecretRef()));
        if (AdminCodigos.blankToNull(cuenta.getClaveCifrada()) == null && cuenta.getSecretRef() == null) {
            throw new AdminValidationException("Indique la clave de la cuenta (o un secret_ref con el nombre de la variable de entorno)");
        }
    }
}
