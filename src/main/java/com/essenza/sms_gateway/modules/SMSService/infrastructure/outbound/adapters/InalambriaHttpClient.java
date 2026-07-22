package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SMSService.infrastructure.config.InalambriaAccountsProperties;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.config.SmsProvidersProperties;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.inalambria.InalambriaHttpRequestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Especialista en comunicación HTTP con la API de Inalambria
 * 
 * Single Responsibility: ÚNICAMENTE ejecutar comunicación HTTP
 * - NO construye payloads
 * - NO transforma respuestas
 * - DELEGA la construcción de peticiones al mapper MapStruct especializado
 */
@Slf4j
@Component
public class InalambriaHttpClient {

    private final RestTemplate restTemplate;
    private final SmsProvidersProperties props;
    private final InalambriaAccountsProperties accountsProps;
    private final InalambriaHttpRequestMapper mapper;

    public InalambriaHttpClient(
            @Qualifier("inalambriaRestTemplate") RestTemplate restTemplate,
            SmsProvidersProperties smsProviderProperties,
            InalambriaAccountsProperties accountsProperties,
            InalambriaHttpRequestMapper mapper) {
        this.restTemplate = restTemplate;
        this.props = smsProviderProperties;
        this.accountsProps = accountsProperties;
        this.mapper = mapper;
        
        // Inicializar configuración por defecto si es necesario
        accountsProps.initializeDefaults();
        
        // Log de configuración para diagnóstico
        var providerConfig = props.getProviderConfig("inalambria");
        if (providerConfig != null) {
            log.info("[HTTP-CLIENT] Configuración Inalambria - URL: {}, Enabled: {}", 
                    providerConfig.getBaseUrl(), 
                    providerConfig.isEnabled());
        } else {
            log.warn("[HTTP-CLIENT] No se encontró configuración para provider 'inalambria'");
        }
    }

    /**
     * Envía petición HTTP a la API de Inalambria usando credenciales específicas por tipo
     */
    public ResponseEntity<HashMap<String, Object>> sendRequest(HashMap<String, Object> payload, String accountType) throws RestClientException {
        // Obtener configuración base del provider
        var providerConfig = props.getProviderConfig("inalambria");
        String baseUrl = providerConfig != null ? providerConfig.getBaseUrl() : "https://restaws.inalambria.com";
        
        // Obtener credenciales específicas para el tipo de cuenta
        var accountConfig = accountsProps.getAccountConfigWithFallback(accountType);
        String username = accountConfig.getUsername();
        String password = accountConfig.getPassword();
        
        // Usar el endpoint correcto para Inalambria
        String endpointUrl = baseUrl + "/MTMessage";
        var request = mapper.mapToHttpRequest(payload, username, password);
        
        log.info("[HTTP-CLIENT] Enviando petición a: {} con cuenta: {} (tipo: {})", endpointUrl, username, accountType);
        return restTemplate.postForEntity(endpointUrl, request, (Class<HashMap<String, Object>>) (Class<?>) HashMap.class);
    }

    /**
     * Verifica disponibilidad del servicio y de las cuentas
     */
    public boolean isServiceAvailable() {
        var providerConfig = props.getProviderConfig("inalambria");
        if (providerConfig == null) return false;
        
        boolean enabled = providerConfig.isEnabled();
        boolean hasBaseUrl = providerConfig.getBaseUrl() != null && !providerConfig.getBaseUrl().trim().isEmpty();
        
        // Verificar que al menos haya una cuenta configurada
        boolean hasAccounts = !accountsProps.getAccounts().isEmpty();
        
        log.debug("[HTTP-CLIENT] Disponibilidad: enabled={}, hasUrl={}, hasAccounts={}", 
                enabled, hasBaseUrl, hasAccounts);
        
        return enabled && hasBaseUrl && hasAccounts;
    }
    
    /**
     * Verifica disponibilidad de un tipo de cuenta específico
     */
    public boolean isAccountAvailable(String accountType) {
        return isServiceAvailable() && accountsProps.isAccountEnabled(accountType);
    }
}
