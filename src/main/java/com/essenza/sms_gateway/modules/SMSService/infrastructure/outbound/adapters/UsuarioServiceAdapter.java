package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Usuario.ActualizarUsuarioDto;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Usuario.ActualizarUsuarioResult;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.UsuarioServicePort;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.config.UsuarioServiceProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

/**
 * Adaptador para consumir el servicio externo de usuarios
 * Implementa el puerto UsuarioServicePort
 */
@Slf4j
@Component
public class UsuarioServiceAdapter implements UsuarioServicePort {

    private final RestTemplate restTemplate;
    private final UsuarioServiceProperties properties;
    private final ObjectMapper objectMapper;

    public UsuarioServiceAdapter(RestTemplateBuilder restTemplateBuilder, 
                                  UsuarioServiceProperties properties) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .build();
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    public ActualizarUsuarioResult guardarUsuario(ActualizarUsuarioDto usuario) {
        if (!properties.getEnabled()) {
            log.warn("[USUARIO-SERVICE] Servicio deshabilitado");
            return ActualizarUsuarioResult.failure("Servicio de usuarios deshabilitado", "SERVICE_DISABLED");
        }

        if (usuario == null || !usuario.isValid()) {
            log.error("[USUARIO-SERVICE] Datos de usuario inválidos: {}", usuario);
            return ActualizarUsuarioResult.failure("Datos de usuario inválidos", "VALIDATION_ERROR");
        }

        String url = properties.getGuardarUrl();
        log.info("[USUARIO-SERVICE] ========================================");
        log.info("[USUARIO-SERVICE] URL completa: {}", url);
        log.info("[USUARIO-SERVICE] Base URL: {}", properties.getBaseUrl());
        log.info("[USUARIO-SERVICE] Endpoint: {}", properties.getGuardarEndpoint());
        log.info("[USUARIO-SERVICE] Usuario: {}", maskIdentificacion(usuario.getNroIdentificacion()));
        log.info("[USUARIO-SERVICE] ========================================");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Propagar token JWT del request original
            String authToken = getAuthorizationToken();
            if (authToken != null) {
                headers.set("Authorization", authToken);
                log.debug("[USUARIO-SERVICE] Token JWT propagado al servicio externo");
            } else {
                log.warn("[USUARIO-SERVICE] No se encontró token JWT en el request original");
            }

            String jsonBody = objectMapper.writeValueAsString(usuario);
            log.debug("[USUARIO-SERVICE] Request body: {}", maskSensitiveData(jsonBody));

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[USUARIO-SERVICE] ✅ Usuario actualizado exitosamente: {}", 
                        maskIdentificacion(usuario.getNroIdentificacion()));
                return ActualizarUsuarioResult.success("Usuario actualizado exitosamente");
            } else {
                log.error("[USUARIO-SERVICE] ❌ Error inesperado: {}", response.getStatusCode());
                return ActualizarUsuarioResult.failure(
                        "Error al actualizar usuario: " + response.getStatusCode(),
                        "UNEXPECTED_RESPONSE"
                );
            }

        } catch (HttpClientErrorException e) {
            log.error("[USUARIO-SERVICE] ❌ Error de cliente (4xx): {} - {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return ActualizarUsuarioResult.failure(
                    "Error de validación: " + e.getResponseBodyAsString(),
                    "CLIENT_ERROR_" + e.getStatusCode().value()
            );
        } catch (HttpServerErrorException e) {
            log.error("[USUARIO-SERVICE] ❌ Error de servidor (5xx): {} - {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return ActualizarUsuarioResult.failure(
                    "Error del servidor de usuarios",
                    "SERVER_ERROR_" + e.getStatusCode().value()
            );
        } catch (ResourceAccessException e) {
            log.error("[USUARIO-SERVICE] ❌ Error de conexión: {}", e.getMessage());
            return ActualizarUsuarioResult.failure(
                    "No se pudo conectar al servicio de usuarios",
                    "CONNECTION_ERROR"
            );
        } catch (Exception e) {
            log.error("[USUARIO-SERVICE] ❌ Error inesperado: {}", e.getMessage(), e);
            return ActualizarUsuarioResult.failure(
                    "Error interno: " + e.getMessage(),
                    "INTERNAL_ERROR"
            );
        }
    }

    @Override
    public boolean isAvailable() {
        if (!properties.getEnabled()) {
            return false;
        }

        try {
            String healthUrl = properties.getBaseUrl() + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("[USUARIO-SERVICE] Health check falló: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Enmascara el número de identificación para logs
     */
    private String maskIdentificacion(String identificacion) {
        if (identificacion == null || identificacion.length() < 4) {
            return "****";
        }
        return "****" + identificacion.substring(identificacion.length() - 4);
    }

    /**
     * Enmascara datos sensibles en el JSON para logs
     */
    private String maskSensitiveData(String json) {
        return json.replaceAll("\"pin\"\\s*:\\s*\"[^\"]*\"", "\"pin\":\"****\"")
                   .replaceAll("\"salt\"\\s*:\\s*\"[^\"]*\"", "\"salt\":\"****\"");
    }
    
    /**
     * Obtiene el token de autorización del request HTTP actual
     */
    private String getAuthorizationToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("Authorization");
            }
        } catch (Exception e) {
            log.debug("[USUARIO-SERVICE] No se pudo obtener el token: {}", e.getMessage());
        }
        return null;
    }
}
