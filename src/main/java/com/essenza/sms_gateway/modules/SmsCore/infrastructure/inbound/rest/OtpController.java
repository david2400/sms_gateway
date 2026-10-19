package com.essenza.sms_gateway.modules.SmsCore.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsCore.application.input.SendOtpUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SmsAvailabilityUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de OTP. Mantienen las mismas URLs y el mismo contrato que antes,
 * pero ahora envían por el motor de SmsCore (ruta CENTRAL tipo OTP, 3 intentos por
 * cuenta, cambio de cuenta/proveedor y log). {@code message_id} = id del envío en el log.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "OTP", description = "Envío de códigos OTP y OTP con actualización de PIN")
public class OtpController {

    private final SendOtpUseCase sendOtpUseCase;
    private final SmsAvailabilityUseCase availabilityUseCase;

    @Operation(summary = "Enviar OTP personalizado", description = "El cliente envía el código y el mensaje ({OTP} se reemplaza)")
    @PostMapping("/otp/send")
    public ResponseEntity<OtpSendResult> sendOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(
                    value = "{\"phone_number\":\"3001234567\",\"otp_code\":\"456789\",\"expiration_minutes\":5,\"message\":\"Su código es: {OTP}\"}")))
            @Valid @RequestBody SendOtpRequest request,
            @Parameter(description = "Llave para evitar envíos duplicados")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return toResponse(sendOtpUseCase.send(request, idempotencyKey));
    }

    @Operation(summary = "Enviar OTP auto-generado", description = "El servicio genera el código y lo devuelve en la respuesta")
    @PostMapping("/otp/send-auto")
    public ResponseEntity<OtpSendResult> sendOtpAutoGenerate(
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String message,
            @RequestParam(required = false, defaultValue = "5") Integer expirationMinutes) {
        String finalMessage = message != null && !message.isBlank()
                ? message : "Your verification code is: {OTP}. Valid for " + expirationMinutes + " minutes.";
        return toResponse(sendOtpUseCase.sendAutoGenerate(phoneNumber, finalMessage, expirationMinutes));
    }

    @Operation(summary = "Enviar OTP simple", description = "Código auto-generado con mensaje por defecto")
    @PostMapping("/otp/send-simple")
    public ResponseEntity<OtpSendResult> sendSimpleOtp(@RequestParam String phoneNumber) {
        return toResponse(sendOtpUseCase.sendWithDefaultMessage(phoneNumber));
    }

    @Operation(summary = "Enviar OTP con PIN",
            description = "Genera el PIN, lo envía por SMS y lo actualiza en el servicio de usuarios")
    @PostMapping("/otp/send-with-pin")
    public ResponseEntity<OtpSendResult> sendOtpWithPin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(
                    value = "{\"phone_number\":\"3001234567\",\"tipo_identificacion\":\"CC\",\"nro_identificacion\":\"123456789\",\"nombre\":\"Juan Pérez\",\"message\":\"Su código de acceso es: {OTP}\",\"expiration_minutes\":5}")))
            @Valid @RequestBody SendOtpRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        request.setWithPinUpdate(true);
        OtpSendResult result = sendOtpUseCase.send(request, idempotencyKey);
        if (!result.isSuccess() && result.isOtpSent()) {
            // El SMS salió pero falló la actualización del PIN: se responde 200 con el detalle (igual que antes)
            return ResponseEntity.ok(result);
        }
        return toResponse(result);
    }

    @Operation(summary = "Estado del servicio OTP", description = "Disponible si hay ruta central OTP con cuentas activas")
    @GetMapping("/otp/status")
    public ResponseEntity<OtpStatusResponse> checkStatus() {
        return ResponseEntity.ok(OtpStatusResponse.builder()
                .available(availabilityUseCase.isAvailable(MessageType.OTP))
                .provider(availabilityUseCase.primaryProvider(MessageType.OTP).orElse("Sin ruta OTP"))
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private static ResponseEntity<OtpSendResult> toResponse(OtpSendResult result) {
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @Builder
    @Data
    public static class OtpStatusResponse {
        private boolean available;
        private String provider;
        private long timestamp;
    }
}
