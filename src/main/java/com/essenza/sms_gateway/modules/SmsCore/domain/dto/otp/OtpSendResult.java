package com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Resultado del envío de mensaje OTP
 */
@Data
@Builder
@Schema(
        description = "Resultado del envío de mensaje OTP"
)
public class OtpSendResult {
    
    @Schema(description = "Indica si el envío fue exitoso", example = "true")
    private boolean success;
    
    @Schema(description = "ID del mensaje", example = "MSG-12345")
    private String messageId;
    
    @Schema(description = "Referencia del proveedor", example = "PROV-REF-67890")
    private String providerReference;
    
    @Schema(description = "Número de teléfono destinatario", example = "3157132338")
    private String phoneNumber;
    
    @Schema(description = "Código OTP (solo para logging)", example = "123456")
    private String otpCode;
    
    @Schema(description = "Fecha de envío", example = "2024-01-20T08:00:00Z")
    private Instant sentAt;
    
    @Schema(description = "Mensaje de error si falló", example = "Error de conexión")
    private String errorMessage;
    
    @Schema(description = "Código de error si falló", example = "CONN_ERROR")
    private String errorCode;
    
    @Schema(description = "Tiempo de expiración en minutos", example = "5")
    private Integer expirationMinutes;
    
    @Schema(description = "Fecha de expiración", example = "2024-01-20T08:05:00Z")
    private Instant expiresAt;
    
    // ========== Campos para OTP con PIN ==========
    @Schema(description = "Indica si el OTP fue enviado", example = "true")
    private boolean otpSent;
    
    @Schema(description = "Indica si el PIN fue actualizado en servicio externo", example = "true")
    private boolean pinUpdated;
    
    /**
     * Crea un resultado exitoso (OTP simple)
     */
    public static OtpSendResult success(String messageId, String providerReference, 
                                      String phoneNumber, String otpCode, Integer expirationMinutes) {
        Instant now = Instant.now();
        return OtpSendResult.builder()
                .success(true)
                .otpSent(true)
                .messageId(messageId)
                .providerReference(providerReference)
                .phoneNumber(phoneNumber)
                .otpCode(otpCode) // Solo para logging
                .sentAt(now)
                .expirationMinutes(expirationMinutes)
                .expiresAt(now.plusSeconds(expirationMinutes * 60L))
                .build();
    }
    
    /**
     * Crea un resultado exitoso con actualización de PIN
     */
    public static OtpSendResult successWithPin(String messageId, String providerReference, 
                                               String phoneNumber, Integer expirationMinutes) {
        Instant now = Instant.now();
        return OtpSendResult.builder()
                .success(true)
                .otpSent(true)
                .pinUpdated(true)
                .messageId(messageId)
                .providerReference(providerReference)
                .phoneNumber(phoneNumber)
                .sentAt(now)
                .expirationMinutes(expirationMinutes)
                .expiresAt(now.plusSeconds(expirationMinutes * 60L))
                .build();
    }
    
    /**
     * Crea un resultado donde el OTP se envió pero el PIN no se actualizó
     */
    public static OtpSendResult otpSentPinFailed(String messageId, String providerReference,
                                                  String phoneNumber, Integer expirationMinutes,
                                                  String pinErrorMessage) {
        Instant now = Instant.now();
        return OtpSendResult.builder()
                .success(false)
                .otpSent(true)
                .pinUpdated(false)
                .messageId(messageId)
                .providerReference(providerReference)
                .phoneNumber(phoneNumber)
                .sentAt(now)
                .expirationMinutes(expirationMinutes)
                .expiresAt(now.plusSeconds(expirationMinutes * 60L))
                .errorMessage("OTP enviado pero fallo al actualizar PIN: " + pinErrorMessage)
                .errorCode("PIN_UPDATE_FAILED")
                .build();
    }
    
    /**
     * Crea un resultado fallido
     */
    public static OtpSendResult failure(String errorMessage, String errorCode) {
        return OtpSendResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .sentAt(Instant.now())
                .build();
    }
    
    /**
     * Verifica si el OTP ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Obtiene el tiempo restante en minutos
     */
    public long getRemainingMinutes() {
        if (expiresAt == null) {
            return 0;
        }
        long remainingSeconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remainingSeconds / 60);
    }
    
    /**
     * Verifica si fue una operación con PIN
     */
    public boolean isWithPinOperation() {
        return otpSent || pinUpdated;
    }
}
