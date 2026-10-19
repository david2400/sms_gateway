package com.essenza.sms_gateway.modules.Mock.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.Mock.application.input.MockSendSmsUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioResponse;
import com.essenza.sms_gateway.modules.SmsCore.shared.SmsHttpStatusMapper;
import io.swagger.v3.oas.annotations.Operation;
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
 * Endpoint del proveedor simulado (pruebas). No envía SMS reales.
 */
@RestController
@RequestMapping("/v1/sms/mock")
@RequiredArgsConstructor
@Tag(name = "SMS Mock", description = "Proveedor simulado para pruebas")
public class MockSmsController {

    private final MockSendSmsUseCase sendSmsUseCase;

    @Operation(summary = "Enviar SMS simulado", description = "Usa la ruta MOCK del tipo de mensaje; queda en el log como cualquier envío")
    @PostMapping("/send")
    public ResponseEntity<SmsEnvioResponse> send(
            @Valid @RequestBody SendSmsRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return SmsHttpStatusMapper.toResponse(sendSmsUseCase.send(request, idempotencyKey));
    }
}
