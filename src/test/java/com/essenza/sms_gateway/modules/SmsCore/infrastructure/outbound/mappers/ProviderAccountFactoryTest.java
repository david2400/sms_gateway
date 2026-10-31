package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.config.SmsCoreProperties;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters.AesGcmSecretCipherAdapter;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters.EnvSecretResolver;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.CanalCuentaEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.CanalProveedorEntity;
import com.essenza.sms_gateway.modules.SmsCore.shared.crypto.AesGcmCipher;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProviderAccountFactoryTest {

    private static final String KEY = AesGcmCipher.generateKey();

    private ProviderAccountFactory factory(String key) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("t", Map.of("CLAVE_ENV", "desde-env")));
        SmsCoreProperties props = new SmsCoreProperties();
        props.setCryptoKey(key);
        return new ProviderAccountFactory(new EnvSecretResolver(env), new AesGcmSecretCipherAdapter(props));
    }

    private static CanalCuentaEntity cuenta(String claveCifrada, String secretRef) {
        CanalProveedorEntity p = new CanalProveedorEntity();
        p.setIdCanalProveedor(1L);
        p.setCodigo("INALAMBRIA");
        p.setBaseUrl("https://x");
        CanalCuentaEntity c = new CanalCuentaEntity();
        c.setIdCanalCuenta(7L);
        c.setCodigo("INALAMBRIA_OTP");
        c.setUsuario("usuario");
        c.setCanalProveedor(p);
        c.setClaveCifrada(claveCifrada);
        c.setSecretRef(secretRef);
        return c;
    }

    @Test
    void descifraLaClaveParaEnviar() {
        String enc = new AesGcmCipher(KEY).encrypt("c0qU-prueba");
        ProviderAccount a = factory(KEY).toAccount(cuenta(enc, "CLAVE_ENV"));
        assertEquals("c0qU-prueba", a.getPassword());
    }

    @Test
    void sinClaveCifradaUsaSecretRef() {
        assertEquals("desde-env", factory(KEY).toAccount(cuenta(null, "CLAVE_ENV")).getPassword());
    }

    @Test
    void llaveEquivocadaCaeASecretRef() {
        String enc = new AesGcmCipher(AesGcmCipher.generateKey()).encrypt("x");
        assertEquals("desde-env", factory(KEY).toAccount(cuenta(enc, "CLAVE_ENV")).getPassword());
    }

    @Test
    void sinLlaveYSinSecretRefQuedaSinCredenciales() {
        String enc = new AesGcmCipher(KEY).encrypt("x");
        ProviderAccount a = factory(null).toAccount(cuenta(enc, null));
        assertNull(a.getPassword());
        assertFalse(a.hasCredentials());
    }
}
