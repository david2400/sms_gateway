package com.essenza.sms_gateway.modules.SMSService.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración específica de cuentas Inalambria según manual RESTAWS ver 3.1
 * 
 * Según manual, cada tipo de cuenta tiene credenciales específicas:
 * - OTP: COMFENALCOquindioOTP / c0qU1IO
 * - HABEASDATA: COMFENALCOquindioCOMUNICADOR / Mv.SEdD5  
 * - MASIVO: COMFENALCOquindioMASIVO / M451v0,123
 * - TEMPLATE: fallback a OTP
 * - PERSONALIZADA: fallback a OTP
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sms.providers.inalambria.accounts")
public class InalambriaAccountsProperties {

    /**
     * Configuración por tipo de cuenta
     */
    private Map<String, AccountProperties> accounts = new HashMap<>();

    @Data
    public static class AccountProperties {
        private String username;
        private String password;
        private boolean enabled = true;
        private String description;
    }

    /**
     * Obtiene las credenciales para un tipo de cuenta específico
     */
    public AccountProperties getAccountConfig(String accountType) {
        return accounts.get(accountType.toLowerCase());
    }

    /**
     * Obtiene las credenciales con fallback a cuenta por defecto
     */
    public AccountProperties getAccountConfigWithFallback(String accountType) {
        AccountProperties config = getAccountConfig(accountType);
        if (config == null || !config.isEnabled()) {
            // Fallback a cuenta OTP (usada para TEMPLATE y PERSONALIZADA)
            config = getAccountConfig("otp");
        }
        if (config == null || !config.isEnabled()) {
            // Fallback final a cuenta MASIVO
            config = getAccountConfig("masivo");
        }
        return config;
    }

    /**
     * Verifica si un tipo de cuenta está configurado y habilitado
     */
    public boolean isAccountEnabled(String accountType) {
        AccountProperties config = getAccountConfig(accountType);
        return config != null && config.isEnabled();
    }

    /**
     * Inicializa configuración por defecto según manual si no está configurada
     */
    public void initializeDefaults() {
        if (accounts.isEmpty()) {
            // Configuración según manual RESTAWS ver 3.1
            AccountProperties otp = new AccountProperties();
            otp.setUsername("COMFENALCOquindioOTP");
            otp.setPassword("c0qU1IO");
            otp.setDescription("Cuenta para mensajes OTP");
            accounts.put("otp", otp);

            AccountProperties habeasData = new AccountProperties();
            habeasData.setUsername("COMFENALCOquindioCOMUNICADOR");
            habeasData.setPassword("Mv.SEdD5");
            habeasData.setDescription("Cuenta para mensajes Habeas Data");
            accounts.put("habeasdata", habeasData);

            AccountProperties masivo = new AccountProperties();
            masivo.setUsername("COMFENALCOquindioMASIVO");
            masivo.setPassword("M451v0,123");
            masivo.setDescription("Cuenta para mensajes masivos/normales");
            accounts.put("masivo", masivo);

            // TEMPLATE y PERSONALIZADA usan OTP por defecto
            AccountProperties template = new AccountProperties();
            template.setUsername("COMFENALCOquindioOTP");
            template.setPassword("c0qU1IO");
            template.setDescription("Cuenta para plantillas (usa OTP)");
            accounts.put("template", template);

            AccountProperties personalizada = new AccountProperties();
            personalizada.setUsername("COMFENALCOquindioOTP");
            personalizada.setPassword("c0qU1IO");
            personalizada.setDescription("Cuenta para mensajes personalizados (usa OTP)");
            accounts.put("personalizada", personalizada);
        }
    }
}
