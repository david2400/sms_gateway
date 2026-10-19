package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.usuario.ActualizarUsuarioDto;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.usuario.ActualizarUsuarioResult;

/**
 * Puerto para el servicio externo de usuarios
 * Permite actualizar información de usuario incluyendo PIN
 */
public interface UsuarioServicePort {
    
    /**
     * Actualiza o guarda información del usuario con PIN
     * @param usuario DTO con los datos del usuario
     * @return Resultado de la operación
     */
    ActualizarUsuarioResult guardarUsuario(ActualizarUsuarioDto usuario);
    
    /**
     * Verifica si el servicio está disponible
     * @return true si el servicio está disponible
     */
    boolean isAvailable();
}
