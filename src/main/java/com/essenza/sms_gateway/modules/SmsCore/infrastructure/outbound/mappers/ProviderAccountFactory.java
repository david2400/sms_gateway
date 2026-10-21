package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SecretCipherPort;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters.EnvSecretResolver;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsCuentaEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsProveedorEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Construye la {@link ProviderAccount} de dominio a partir de {@code sms_cuenta} + {@code sms_proveedor}.
 * <ul>
 *   <li>La contraseña se descifra de {@code clave_cifrada}; si no hay (o no se puede descifrar) se usa
 *       {@code secret_ref} (variable de entorno).</li>
 *   <li>Si el usuario empieza por {@code env:} también se resuelve desde el entorno
 *       (útil cuando el "usuario" es una API key, ej. Go4Clients: {@code env:GO4CLIENTS_API_KEY}).</li>
 *   <li>{@code parametros_json} se convierte a un mapa clave→texto.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderAccountFactory {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ENV_PREFIX = "env:";

    private final EnvSecretResolver secretResolver;
    private final SecretCipherPort secretCipher;

    public ProviderAccount toAccount(SmsCuentaEntity cuenta) {
        SmsProveedorEntity proveedor = cuenta.getProveedor();
        return ProviderAccount.builder()
                .id(cuenta.getIdSmsCuenta())
                .code(cuenta.getCodigo())
                .providerId(proveedor.getIdSmsProveedor())
                .providerCode(proveedor.getCodigo())
                .baseUrl(proveedor.getBaseUrl())
                .connectTimeoutMs(proveedor.getConnectTimeoutMs() != null ? proveedor.getConnectTimeoutMs() : 2000)
                .readTimeoutMs(proveedor.getReadTimeoutMs() != null ? proveedor.getReadTimeoutMs() : 5000)
                .username(resolveUsername(cuenta.getUsuario()))
                .password(resolvePassword(cuenta))
                .sender(cuenta.getRemitente())
                .parameters(parseParameters(cuenta))
                .build();
    }

    public boolean isUsable(SmsCuentaEntity cuenta) {
        return Boolean.TRUE.equals(cuenta.getActivo())
                && cuenta.getProveedor() != null
                && Boolean.TRUE.equals(cuenta.getProveedor().getActivo());
    }

    private String resolvePassword(SmsCuentaEntity cuenta) {
        String cifrada = cuenta.getClaveCifrada();
        if (cifrada != null && !cifrada.isBlank()) {
            try {
                return secretCipher.decrypt(cifrada.trim());
            } catch (RuntimeException e) {
                // Nunca se registra la clave, solo el motivo
                log.error("[SMS-CORE] No se pudo descifrar la clave de la cuenta {}: {}{}", cuenta.getCodigo(), e.getMessage(),
                        cuenta.getSecretRef() != null ? " (se intenta con secret_ref)" : "");
            }
        }
        return secretResolver.resolve(cuenta.getSecretRef());
    }

    private String resolveUsername(String usuario) {
        if (usuario != null && usuario.startsWith(ENV_PREFIX)) {
            return secretResolver.resolve(usuario.substring(ENV_PREFIX.length()));
        }
        return usuario;
    }

    private Map<String, String> parseParameters(SmsCuentaEntity cuenta) {
        String json = cuenta.getParametrosJson();
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> raw = JSON.readValue(json, new TypeReference<Map<String, Object>>() { });
            Map<String, String> result = new LinkedHashMap<>();
            raw.forEach((k, v) -> result.put(k, v == null ? null : String.valueOf(v)));
            return result;
        } catch (Exception e) {
            log.error("[SMS-CORE] parametros_json inválido en la cuenta {}: {}", cuenta.getCodigo(), e.getMessage());
            return Collections.emptyMap();
        }
    }
}
