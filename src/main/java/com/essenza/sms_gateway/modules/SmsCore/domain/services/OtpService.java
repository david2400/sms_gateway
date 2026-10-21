package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.usuario.ActualizarUsuarioDto;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.usuario.ActualizarUsuarioResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.UsuarioServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Reglas de negocio de OTP sobre el motor de SmsCore.
 * <p>
 * El OTP se envía por la ruta CENTRAL del tipo {@link MessageType#OTP} (3 intentos por
 * cuenta y cambio de cuenta/proveedor según BD) y queda en el log de envíos.
 * En el flujo con PIN, tras enviar el SMS se actualiza el PIN en el servicio de usuarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    static final String DEFAULT_PIN_MESSAGE = "Su código de acceso Comfenalco es: {OTP}";

    private final SmsDispatchService dispatchService;
    private final PinGeneratorService pinGenerator;
    private final UsuarioServicePort usuarioService;

    /**
     * Envía un OTP. Si {@code withPinUpdate} está activo, genera el PIN y lo actualiza en el servicio de usuarios.
     */
    public OtpSendResult send(SendOtpRequest request, String idempotencyKey) {
        if (request == null) {
            return OtpSendResult.failure("Request inválido", "VALIDATION_ERROR");
        }
        if (request.isWithPinUpdate()) {
            return sendWithPin(request, idempotencyKey);
        }
        if (!request.isValid()) {
            return OtpSendResult.failure("Teléfono, mensaje u otpCode (4 a 8 dígitos) inválidos", "VALIDATION_ERROR");
        }
        DispatchResult result = dispatch(request, idempotencyKey);
        if (!result.isSent()) {
            return failure(result);
        }
        return withDispatch(OtpSendResult.success(result.getEnvioId().toString(), reference(result),
                request.getPhoneNumber(), request.getOtpCode(), expiration(request)), result);
    }

    /**
     * Genera el código y envía el OTP.
     */
    public OtpSendResult sendAutoGenerate(String phoneNumber, String message, Integer expirationMinutes) {
        return sendAutoGenerate(phoneNumber, message, expirationMinutes, null);
    }

    /**
     * Genera el código y envía el OTP, opcionalmente con una cuenta específica (luego el resto de la ruta OTP).
     */
    public OtpSendResult sendAutoGenerate(String phoneNumber, String message, Integer expirationMinutes, String cuenta) {
        SendOtpRequest request = SendOtpRequest.builder()
                .phoneNumber(phoneNumber)
                .otpCode(pinGenerator.generatePin())
                .message(message != null && !message.isBlank() ? message : "Tu código de verificación es: {OTP}")
                .expirationMinutes(expirationMinutes != null ? expirationMinutes : 5)
                .referenceId("AUTO-" + System.currentTimeMillis())
                .cuenta(cuenta)
                .build();
        return send(request, null);
    }

    private OtpSendResult sendWithPin(SendOtpRequest request, String idempotencyKey) {
        if (!request.isValidForPinUpdate()) {
            return OtpSendResult.failure("Datos de usuario incompletos para actualizar PIN", "VALIDATION_ERROR");
        }

        PinGeneratorService.PinData pinData = pinGenerator.generatePinWithSalt();
        SendOtpRequest otpRequest = request.toBuilder()
                .otpCode(pinData.getPin())
                .message(request.getMessage() != null && !request.getMessage().isBlank()
                        ? request.getMessage() : DEFAULT_PIN_MESSAGE)
                .referenceId(request.getReferenceId() != null ? request.getReferenceId() : "PIN-" + System.currentTimeMillis())
                .withPinUpdate(false)
                .build();

        DispatchResult result = dispatch(otpRequest, idempotencyKey);
        if (!result.isSent()) {
            log.error("[OTP] Fallo al enviar OTP con PIN (envío {}): {}", result.getEnvioId(), result.getDetail());
            return failure(result);
        }

        ActualizarUsuarioResult usuario = usuarioService.guardarUsuario(ActualizarUsuarioDto.builder()
                .tipoIdentificacion(request.getTipoIdentificacion())
                .nroIdentificacion(request.getNroIdentificacion())
                .nombre(request.getNombre())
                .pin(pinData.getPin())
                .build());

        String messageId = result.getEnvioId().toString();
        if (!usuario.isSuccess()) {
            log.warn("[OTP] OTP enviado (envío {}) pero no se actualizó el PIN: {}", messageId, usuario.getMessage());
            return withDispatch(OtpSendResult.otpSentPinFailed(messageId, reference(result), request.getPhoneNumber(),
                    expiration(request), usuario.getMessage()), result);
        }
        return withDispatch(OtpSendResult.successWithPin(messageId, reference(result), request.getPhoneNumber(),
                expiration(request)), result);
    }

    private DispatchResult dispatch(SendOtpRequest request, String idempotencyKey) {
        SmsDispatch message = SmsDispatch.builder()
                .to(request.getPhoneNumber())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                .text(request.buildFullMessage())
                .type(MessageType.OTP)
                .accountCode(request.getCuenta())
                .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim())
                .build();
        return dispatchService.dispatch(message, DispatchChannel.CENTRAL, null);
    }

    private static OtpSendResult failure(DispatchResult result) {
        OtpSendResult failure = OtpSendResult.failure(
                result.getDetail() != null ? result.getDetail() : "No se pudo enviar el OTP",
                result.getErrorCode());
        failure.setMessageId(result.getEnvioId() != null ? result.getEnvioId().toString() : null);
        return failure;
    }

    private static OtpSendResult withDispatch(OtpSendResult otp, DispatchResult result) {
        otp.setProviderReference(reference(result));
        return otp;
    }

    /** Proveedor/cuenta con los que salió el SMS (el detalle completo está en /v1/sms/envios/{messageId}). */
    private static String reference(DispatchResult result) {
        return result.getProviderCode() + "/" + result.getAccountCode();
    }

    private static int expiration(SendOtpRequest request) {
        return request.getExpirationMinutes() != null ? request.getExpirationMinutes() : 5;
    }
}
