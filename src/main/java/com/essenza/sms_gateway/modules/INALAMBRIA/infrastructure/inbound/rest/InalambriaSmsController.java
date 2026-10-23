package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms.InalambriaSendBatchUseCase;
import com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms.InalambriaSendSmsUseCase;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.Sms.InalambriaSendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsBatchRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsBatchResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioResponse;
import com.essenza.sms_gateway.modules.SmsCore.shared.SmsHttpStatusMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint propio de Inalambria: usa solo cuentas de Inalambria según la ruta
 * (INALAMBRIA, tipo de mensaje) en BD, con 3 intentos por cuenta y log.
 */
@RestController
@RequestMapping("/v1/sms/inalambria")
@RequiredArgsConstructor
@Tag(name = "SMS Inalambria", description = "Envío de SMS solo por Inalambria")
public class InalambriaSmsController {

    private final InalambriaSendSmsUseCase sendSmsUseCase;
    private final InalambriaSendBatchUseCase sendBatchUseCase;

    @Operation(summary = "Enviar SMS por Inalambria",
            description = "La cuenta (OTP, MASIVO, HABEASDATA...) la decide la ruta INALAMBRIA del tipo de mensaje.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMS enviado"),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "422", description = "Inalambria no tiene el servicio del tipo de mensaje, no hay ruta o la plantilla no existe"),
            @ApiResponse(responseCode = "502", description = "Todas las cuentas de Inalambria fallaron")
    })
    @PostMapping("/send")
    public ResponseEntity<SmsEnvioResponse> send(
            @Valid @RequestBody InalambriaSendSmsRequest request,
            @Parameter(description = "Llave para evitar envíos duplicados")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return SmsHttpStatusMapper.toResponse(sendSmsUseCase.send(request, idempotencyKey));
    }

    @Operation(summary = "Envío masivo por Inalambria (API masiva)",
            description = "Usa la ruta INALAMBRIA del tipo (normalmente MASIVO) y la API /mtmessage/massive en lotes de "
                    + "sms.inalambria.massive.chunk-size (500). Texto igual para todos ('message') o personalizado por "
                    + "destinatario ('plantilla' + variables). Cada destinatario queda como un envío en el log. "
                    + "Si un lote agota sus intentos en una cuenta pasa completo a la siguiente cuenta de la ruta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado por destinatario"),
            @ApiResponse(responseCode = "400", description = "Request o variables inválidas (no se envía nada)"),
            @ApiResponse(responseCode = "422", description = "Plantilla inexistente")
    })
    @PostMapping("/batch")
    public ResponseEntity<SmsBatchResponse> sendBatch(@Valid @RequestBody SendSmsBatchRequest request) {
        return ResponseEntity.ok(sendBatchUseCase.send(request));
    }
}
