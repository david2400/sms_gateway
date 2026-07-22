package com.essenza.sms_gateway.modules.SMSService.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SmsTypeRequest;
import com.essenza.sms_gateway.modules.SMSService.application.input.Sms.ManageSmsProvidersUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Sms.SendSmsBatchUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Sms.SendSmsWithTypeUseCase;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SMSResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@Tag(name = "SMS Service", description = "API para envío de SMS")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class SmsController {

    //    private final SendOtpUseCase sendOtpUseCase;
    private final SendSmsBatchUseCase sendSmsBatchUseCase;
    private final ManageSmsProvidersUseCase manageSmsProvidersUseCase;
    //    private final SendSmsUseCase sendSmsUseCase;
    private final SendSmsWithTypeUseCase sendSmsWithTypeService;
    //    private final InalambriaAccountOrchestrator accountOrchestrator;

    /**
     * Endpoint unificado para enviar SMS
     * POST /api/notificacion/sms/send
     */
    @Operation(
            summary = "Enviar SMS individual",
            description = "Envía un mensaje SMS individual con tipo específico (NORMAL, OTP, HABEAS_DATA, TEMPLATE, PERSONALIZED)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SMS enviado exitosamente",
                    content = @Content(schema = @Schema(implementation = SMSResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en el envío del SMS",
                    content = @Content(schema = @Schema(implementation = SMSResult.class))
            )
    })
    @PostMapping("/sms/send")
    public ResponseEntity<SMSResult> sendSms(@Valid @RequestBody SmsTypeRequest request) {
        log.info("[SMS-API] Enviando SMS tipo: {} a: {}", request.getSmsType(), request.getTo());

        // Usar el provider directamente para pasar el tipo de cuenta
        SMSResult result = sendSmsWithTypeService.sendWithType(request);

        if (result.isSuccess()) {
            log.info("[SMS-API] ✅ SMS enviado. ID: {}", result.getMessageId());
            return ResponseEntity.ok(result);
        } else {
            log.warn("[SMS-API] ❌ Error enviando SMS: {}", result.getErrorMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }


    /**
     * Endpoint para enviar SMS en batch
     * POST /api/notificacion/sms/batch
     */
    @Operation(
            summary = "Enviar SMS masivo",
            description = "Envía mensajes SMS a múltiples destinatarios en una sola campaña"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Batch enviado exitosamente",
                    content = @Content(schema = @Schema(implementation = SmsBatchResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en el envío del batch",
                    content = @Content(schema = @Schema(implementation = SmsBatchResult.class))
            )
    })
    @PostMapping("/sms/batch")
    public ResponseEntity<SmsBatchResult> sendBatch(@Valid @RequestBody SendSmsBatchCommand command) {
        log.info("[SMS-API] Enviando batch a {} destinatarios", command.getDestinations().size());

        SmsBatchResult result = sendSmsBatchUseCase.send(command);

        if (result.isSuccess()) {
            log.info("[SMS-API] ✅ Batch enviado. Éxitos: {}", result.getAcceptedCount());
            return ResponseEntity.ok(result);
        } else {
            log.warn("[SMS-API] ❌ Error enviando batch: {}", result.getErrorMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Endpoint de health check
     * GET /api/notificacion/sms/health
     */
    @Operation(
            summary = "Verificar estado del servicio SMS",
            description = "Retorna el estado del servicio SMS y el proveedor actual"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado del servicio obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    @GetMapping("/sms/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new java.util.HashMap<>();
        health.put("service_available", manageSmsProvidersUseCase.isCurrentProviderAvailable());
        health.put("provider_name", manageSmsProvidersUseCase.getCurrentProvider());
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}

