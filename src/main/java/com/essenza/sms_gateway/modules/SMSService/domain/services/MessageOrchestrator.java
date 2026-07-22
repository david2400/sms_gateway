package com.essenza.sms_gateway.modules.SMSService.domain.services;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSMSCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SmsTypeRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Usuario.ActualizarUsuarioDto;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SMSResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.MessageRouterPort;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.UsuarioServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orquestador de dominio para el envío de mensajes (SMS, SMS con tipo, OTP y batch).
 * <p>
 * Es el "cerebro" que decide únicamente CÓMO se ejecuta un envío: delega en
 * {@link MessageRouterPort} la selección del proveedor (routing/failover/reintentos)
 * y añade las reglas de negocio propias del dominio (ej. actualización de PIN tras
 * un OTP exitoso). Nunca conoce proveedores concretos: solo trabaja contra
 * {@link com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageOrchestrator {

    private final MessageRouterPort router;
    private final UsuarioServicePort usuarioServicePort;
    private final PinGeneratorService pinGeneratorService;

    // ========== SMS ==========

    public SMSResult sendSms(SendSMSCommand command) {
        log.info("[ORCHESTRATOR] Enviando SMS a {}", command.getTo());

        if (command == null || !command.isValid()) {
            log.error("[ORCHESTRATOR] Comando inválido: {}", command);
            return SMSResult.failure("Comando inválido", "VALIDATION_ERROR");
        }

        return router.execute(
                provider -> provider.sendSms(command),
                errorMsg -> SMSResult.failure(errorMsg, "ROUTER_ERROR")
        );
    }

    public SMSResult sendSmsWithType(SmsTypeRequest request) {
        log.info("[ORCHESTRATOR] Enviando SMS tipo {} a {}", request.getSmsType(), request.getTo());

        if (request == null || !request.isValid()) {
            return SMSResult.failure("Request inválido", "VALIDATION_ERROR");
        }

        return router.execute(
                provider -> provider.sendSmsWithType(request),
                errorMsg -> SMSResult.failure(errorMsg, "ROUTER_ERROR")
        );
    }

    // ========== Batch ==========

    public SmsBatchResult sendBatch(SendSmsBatchCommand command) {
        log.info("[ORCHESTRATOR] Enviando batch a {} destinatarios", command.getDestinations().size());

        if (command == null || !command.isValid()) {
            return SmsBatchResult.failure("Comando inválido", "VALIDATION_ERROR");
        }

        return router.execute(
                provider -> {
                    var result = provider.sendBatch(command);
                    logResult("Batch", result.isSuccess(), result.getCampaignId(), result.getErrorMessage());
                    return result;
                },
                errorMsg -> SmsBatchResult.failure(errorMsg, "ROUTER_ERROR")
        );
    }

    // ========== OTP ==========

    public OtpSendResult sendOtp(SendOtpRequest request) {
        log.info("[ORCHESTRATOR] Enviando OTP a {}", request.getPhoneNumber());

        if (request == null || !request.isValid()) {
            return OtpSendResult.failure("Request inválido", "VALIDATION_ERROR");
        }

        if (request.isWithPinUpdate()) {
            return sendOtpWithPinUpdate(request);
        }

        return router.execute(
                provider -> {
                    if (!provider.validatePhoneNumber(request.getPhoneNumber())) {
                        return OtpSendResult.failure("Teléfono inválido", "INVALID_PHONE");
                    }
                    var result = provider.sendOtp(request);
                    logResult("OTP", result.isSuccess(), result.getMessageId(), result.getErrorMessage());
                    return result;
                },
                errorMsg -> OtpSendResult.failure(errorMsg, "ROUTER_ERROR")
        );
    }

    /**
     * Envía OTP y actualiza PIN en el servicio externo de usuarios
     */
    private OtpSendResult sendOtpWithPinUpdate(SendOtpRequest request) {
        log.info("[ORCHESTRATOR-PIN] Procesando OTP con PIN para: {}", maskPhone(request.getPhoneNumber()));

        if (!request.isValidForPinUpdate()) {
            log.error("[ORCHESTRATOR-PIN] Request inválido para actualización de PIN");
            return OtpSendResult.failure("Datos de usuario incompletos para actualizar PIN", "VALIDATION_ERROR");
        }

        // 1. Generar PIN y salt
        PinGeneratorService.PinData pinData = pinGeneratorService.generatePinWithSalt();
        String pin = pinData.getPin();

        log.info("[ORCHESTRATOR-PIN] PIN generado para usuario: {}", maskIdentificacion(request.getNroIdentificacion()));

        // 2. Crear request interno para enviar OTP
        SendOtpRequest otpRequest = SendOtpRequest.builder()
                .phoneNumber(request.getPhoneNumber())
                .otpCode(pin)
                .message(request.getMessage())
                .expirationMinutes(request.getExpirationMinutes())
                .referenceId("PIN-" + System.currentTimeMillis())
                .withPinUpdate(false) // Evitar recursividad
                .build();

        // 3. Enviar OTP por SMS
        OtpSendResult otpResult = router.execute(
                provider -> {
                    if (!provider.validatePhoneNumber(otpRequest.getPhoneNumber())) {
                        return OtpSendResult.failure("Teléfono inválido", "INVALID_PHONE");
                    }
                    return provider.sendOtp(otpRequest);
                },
                errorMsg -> OtpSendResult.failure(errorMsg, "ROUTER_ERROR")
        );

        if (!otpResult.isSuccess()) {
            log.error("[ORCHESTRATOR-PIN] ❌ Fallo al enviar OTP: {}", otpResult.getErrorMessage());
            return otpResult;
        }

        log.info("[ORCHESTRATOR-PIN] ✅ OTP enviado exitosamente. ID: {}", otpResult.getMessageId());

        // 4. Actualizar PIN en el servicio de usuarios
        ActualizarUsuarioDto usuarioDto = ActualizarUsuarioDto.builder()
                .tipoIdentificacion(request.getTipoIdentificacion())
                .nroIdentificacion(request.getNroIdentificacion())
                .nombre(request.getNombre())
                .pin(pin)
                .build();

        var usuarioResult = usuarioServicePort.guardarUsuario(usuarioDto);

        if (!usuarioResult.isSuccess()) {
            log.warn("[ORCHESTRATOR-PIN] ⚠️ OTP enviado pero fallo al actualizar PIN: {}", usuarioResult.getMessage());
            return OtpSendResult.otpSentPinFailed(
                    otpResult.getMessageId(),
                    otpResult.getProviderReference(),
                    request.getPhoneNumber(),
                    request.getExpirationMinutes(),
                    usuarioResult.getMessage()
            );
        }

        log.info("[ORCHESTRATOR-PIN] ✅ Proceso completo exitoso para: {}", maskIdentificacion(request.getNroIdentificacion()));

        return OtpSendResult.successWithPin(
                otpResult.getMessageId(),
                otpResult.getProviderReference(),
                request.getPhoneNumber(),
                request.getExpirationMinutes()
        );
    }

    public String generateOtpCode() {
        var provider = router.getCurrentProvider();
        return provider != null ? provider.generateOtpCode(6) : generateFallbackOtp();
    }

    // ========== Utilidades ==========

    private void logResult(String operation, boolean success, String id, String error) {
        if (success) {
            log.info("[ORCHESTRATOR] ✅ {} exitoso. ID: {}", operation, id);
        } else {
            log.error("[ORCHESTRATOR] ❌ {} fallido: {}", operation, error);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    private String maskIdentificacion(String identificacion) {
        if (identificacion == null || identificacion.length() < 4) {
            return "****";
        }
        return "****" + identificacion.substring(identificacion.length() - 4);
    }

    private String generateFallbackOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append((int) (Math.random() * 10));
        }
        return otp.toString();
    }
}
