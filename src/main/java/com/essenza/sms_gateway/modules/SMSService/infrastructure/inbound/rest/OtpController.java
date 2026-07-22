package com.essenza.sms_gateway.modules.SMSService.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.application.input.Otp.SendOtpAutoGenerateUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Otp.SendOtpDefaultMessageUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Otp.SendOtpUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Sms.SmsServiceAvailabilityUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;


@Tag(name = "OTP", description = "API para OTP")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class OtpController {

    private final SendOtpUseCase sendOtpService;
    private final SendOtpDefaultMessageUseCase sendWithDefaultMessageService;
    private final SmsServiceAvailabilityUseCase smsServiceAvailabilityService;
    private final SendOtpAutoGenerateUseCase sendAutoGenerateService;

    /**
     * Envía un mensaje OTP con datos personalizados
     */
    @Operation(
            summary = "Enviar OTP personalizado",
            description = "Envía un código OTP con mensaje y datos personalizados"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP enviado exitosamente",
                    content = @Content(schema = @Schema(implementation = OtpSendResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en el envío del OTP",
                    content = @Content(schema = @Schema(implementation = OtpSendResult.class))
            )
    })
    @PostMapping("/otp/send")
    public ResponseEntity<OtpSendResult> sendOtp(
            @Parameter(
                    description = "Datos del OTP a enviar", 
                    required = true,
                    examples = {
                            @ExampleObject(
                                    name = "OTP Personalizado",
                                    description = "Ejemplo de OTP con mensaje personalizado",
                                    value = "{\"phoneNumber\":\"573001234567\",\"otpCode\":\"456789\",\"expirationMinutes\":5,\"message\":\"Su código de verificación para Comfenalco es: 456789. Válido por 5 minutos.\"}"
                            ),
                            @ExampleObject(
                                    name = "OTP Bancario",
                                    description = "Ejemplo de OTP para transacción bancaria",
                                    value = "{\"phoneNumber\":\"573001234568\",\"otpCode\":\"123456\",\"expirationMinutes\":10,\"message\":\"Código de seguridad para su transacción: 123456. Nunca comparta este código.\"}"
                            )
                    }
            )
            @Valid @RequestBody SendOtpRequest request) {
        log.info("[OTP-API] Received OTP send request for: {}", request.getPhoneNumber());

        OtpSendResult result = sendOtpService.send(request);

        if (result.isSuccess()) {
            log.info("[OTP-API] ✅ OTP sent successfully. ID: {}", result.getMessageId());
            return ResponseEntity.ok(result);
        } else {
            log.warn("[OTP-API] ❌ OTP send failed: {}", result.getErrorMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Envía un OTP generando el código automáticamente
     */
    @Operation(
            summary = "Enviar OTP auto-generado",
            description = "Genera y envía automáticamente un código OTP"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP auto-generado enviado exitosamente",
                    content = @Content(schema = @Schema(implementation = OtpSendResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en el envío del OTP auto-generado",
                    content = @Content(schema = @Schema(implementation = OtpSendResult.class))
            )
    })
    @PostMapping("/otp/send-auto")
    public ResponseEntity<OtpSendResult> sendOtpAutoGenerate(
            @Parameter(description = "Número de teléfono destinatario", required = true, example = "573001234567")
            @RequestParam String phoneNumber,
            @Parameter(description = "Mensaje personalizado (opcional)", example = "Your verification code is: {OTP}. Valid for 5 minutes.")
            @RequestParam(required = false) String message,
            @Parameter(description = "Minutos de expiración del código", schema = @Schema(defaultValue = "5"), example = "5")
            @RequestParam(required = false, defaultValue = "5") Integer expirationMinutes) {
        
        log.info("[OTP-API] Received auto-generated OTP request for: {}", phoneNumber);

        String finalMessage = (message != null && !message.trim().isEmpty()) ? 
                message : "Your verification code is: {OTP}. Valid for " + expirationMinutes + " minutes.";

        OtpSendResult result = sendAutoGenerateService.sendAutoGenerate(phoneNumber, finalMessage, expirationMinutes);

        if (result.isSuccess()) {
            log.info("[OTP-API] ✅ Auto-generated OTP sent successfully. ID: {}", result.getMessageId());
            return ResponseEntity.ok(result);
        } else {
            log.warn("[OTP-API] ❌ Auto-generated OTP failed: {}", result.getErrorMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Envía un OTP con mensaje por defecto
     */
    @Operation(
            summary = "Enviar OTP simple",
            description = "Envía un OTP con un mensaje predefinido"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP simple enviado exitosamente",
                    content = @Content(schema = @Schema(implementation = OtpSendResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en el envío del OTP simple",
                    content = @Content(schema = @Schema(implementation = OtpSendResult.class))
            )
    })
    @PostMapping("/otp/send-simple")
    public ResponseEntity<OtpSendResult> sendSimpleOtp(
            @Parameter(description = "Número de teléfono destinatario", required = true, example = "573001234567")
            @RequestParam String phoneNumber) {
        log.info("[OTP-API] Received simple OTP request for: {}", phoneNumber);

        OtpSendResult result = sendWithDefaultMessageService.sendWithDefaultMessage(phoneNumber);

        if (result.isSuccess()) {
            log.info("[OTP-API] ✅ Simple OTP sent successfully. ID: {}", result.getMessageId());
            return ResponseEntity.ok(result);
        } else {
            log.warn("[OTP-API] ❌ Simple OTP failed: {}", result.getErrorMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Envía un OTP y actualiza el PIN del usuario en el servicio externo
     */
    @Operation(
            summary = "Enviar OTP con PIN",
            description = "Genera un código PIN, lo envía por SMS y actualiza el PIN del usuario en el servicio de usuarios externo"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP enviado y PIN actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = OtpSendResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en el proceso",
                    content = @Content(schema = @Schema(implementation = OtpSendResult.class))
            )
    })
    @PostMapping("/otp/send-with-pin")
    public ResponseEntity<OtpSendResult> sendOtpWithPin(
            @Parameter(
                    description = "Datos del usuario para enviar OTP y actualizar PIN",
                    required = true,
                    examples = {
                            @ExampleObject(
                                    name = "OTP con PIN",
                                    description = "Ejemplo de envío OTP con actualización de PIN",
                                    value = "{\"phoneNumber\":\"573001234567\",\"tipoIdentificacion\":\"CC\",\"nroIdentificacion\":\"123456789\",\"nombre\":\"Juan Pérez\",\"message\":\"Su código de acceso es: {OTP}\",\"expirationMinutes\":5,\"withPinUpdate\":true}"
                            )
                    }
            )
            @Valid @RequestBody SendOtpRequest request) {
        log.info("[OTP-API] Recibida solicitud OTP con PIN para: {}", maskPhone(request.getPhoneNumber()));

        // Asegurar que withPinUpdate esté activo
        if (!request.isWithPinUpdate()) {
            request = SendOtpRequest.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .message(request.getMessage())
                    .expirationMinutes(request.getExpirationMinutes())
                    .tipoIdentificacion(request.getTipoIdentificacion())
                    .nroIdentificacion(request.getNroIdentificacion())
                    .nombre(request.getNombre())
                    .withPinUpdate(true)
                    .build();
        }

        OtpSendResult result = sendOtpService.send(request);

        if (result.isSuccess()) {
            log.info("[OTP-API] ✅ OTP con PIN procesado exitosamente. ID: {}", result.getMessageId());
            return ResponseEntity.ok(result);
        } else if (result.isOtpSent() && !result.isPinUpdated()) {
            log.warn("[OTP-API] ⚠️ OTP enviado pero PIN no actualizado: {}", result.getErrorMessage());
            return ResponseEntity.ok(result);
        } else {
            log.warn("[OTP-API] ❌ Fallo en OTP con PIN: {}", result.getErrorMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Verifica el estado del servicio OTP
     */
    @Operation(
            summary = "Verificar estado del servicio OTP",
            description = "Retorna el estado de disponibilidad del servicio OTP y proveedor actual"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado del servicio obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = OtpStatusResponse.class))
            )
    })
    @GetMapping("/otp/status")
    public ResponseEntity<OtpStatusResponse> checkStatus() {
        log.debug("[OTP-API] Checking OTP service status");

        boolean available = smsServiceAvailabilityService.isServiceAvailable();
        String provider = smsServiceAvailabilityService.getProviderName();

        OtpStatusResponse status = OtpStatusResponse.builder()
                .available(available)
                .provider(provider)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.ok(status);
    }

    /**
     * DTO para respuesta de estado
     */
    @Schema(description = "Respuesta de estado del servicio OTP")
    @lombok.Builder
    @lombok.Data
    public static class OtpStatusResponse {
        @Schema(description = "Indica si el servicio está disponible")
        private boolean available;
        @Schema(description = "Nombre del proveedor actual")
        private String provider;
        @Schema(description = "Timestamp de la consulta")
        private long timestamp;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}


