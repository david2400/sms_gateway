package com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para enviar un mensaje OTP sin crear notificación
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "Request para enviar mensaje OTP"
)
public class SendOtpRequest {
    
    @NotBlank(message = "El número de teléfono es obligatorio")
    @Size(min = 10, max = 15, message = "El número de teléfono debe tener entre 10 y 15 dígitos")
    @Schema(description = "Número de teléfono destinatario (10 dígitos, sin prefijo país)", example = "3157132338")
    private String phoneNumber;
    
    @Schema(description = "Código de país del número (ej: 57 para Colombia)", example = "57")
    @Builder.Default
    private String countryCode = "57";
    
    @Schema(description = "Código OTP (opcional, se genera automáticamente si withPinUpdate=true)", example = "123456")
    private String otpCode;
    
    @Size(max = 2500, message = "El mensaje no debe exceder 1600 caracteres")
    @Schema(description = "Mensaje personalizado (opcional si withPinUpdate=true)", example = "Su código de acceso es: {OTP}")
    private String message;
    
    @Schema(description = "ID de plantilla OTP (opcional)", example = "TPL001")
    private String templateId;
    
    @Schema(description = "Tiempo de expiración en minutos", example = "5")
    @Builder.Default
    private Integer expirationMinutes = 5;
    
    @Schema(description = "ID de referencia para tracking", example = "REF-12345")
    private String referenceId;
    
    // ========== Campos opcionales para OTP con PIN ==========
    
    @Schema(description = "Tipo de identificación del usuario", example = "CC")
    private String tipoIdentificacion;
    
    @Schema(description = "Número de identificación del usuario", example = "123456789")
    private String nroIdentificacion;
    
    @Schema(description = "Nombre del usuario", example = "Juan Pérez")
    private String nombre;
    
    @Builder.Default
    @Schema(description = "Indica si debe actualizar PIN en servicio externo", example = "false")
    private boolean withPinUpdate = false;
    
    /**
     * Valida que el request sea válido
     */
    public boolean isValid() {
        boolean basicValid = phoneNumber != null && !phoneNumber.trim().isEmpty() &&
               phoneNumber.matches("\\+?\\d{10,15}");
        
        // Si es OTP con PIN, no requiere otpCode ni message (se generan)
        if (withPinUpdate) {
            return basicValid;
        }
        
        // OTP normal requiere otpCode y message
        basicValid = basicValid && 
               message != null && !message.trim().isEmpty() &&
               otpCode != null && !otpCode.trim().isEmpty() &&
               otpCode.matches("\\d{4,8}");
        
        return basicValid;
    }
    
    /**
     * Valida que el request tenga datos de usuario para actualización de PIN
     */
    public boolean isValidForPinUpdate() {
        return isValid() &&
               tipoIdentificacion != null && !tipoIdentificacion.trim().isEmpty() &&
               nroIdentificacion != null && !nroIdentificacion.trim().isEmpty() &&
               nombre != null && !nombre.trim().isEmpty();
    }
    
    /**
     * Texto final del SMS: reemplaza {OTP} o, si no está, agrega el código al final. No modifica el request.
     */
    public String buildFullMessage() {
        if (message.contains("{OTP}")) {
            return message.replace("{OTP}", otpCode);
        }
        return message.trim() + " Su código de acceso Comfenalco es: " + otpCode
                + ". Válido por " + (expirationMinutes != null ? expirationMinutes : 5) + " minutos.";
    }

}
