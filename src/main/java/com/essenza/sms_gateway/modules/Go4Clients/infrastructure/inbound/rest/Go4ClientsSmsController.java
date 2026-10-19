package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.Go4Clients.application.input.Go4ClientsCampaignUseCase;
import com.essenza.sms_gateway.modules.Go4Clients.application.input.Go4ClientsSendSmsUseCase;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.CreateGo4ClientsCampaignRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.SendGo4ClientsCampaignMessagesRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioResponse;
import com.essenza.sms_gateway.modules.SmsCore.shared.SmsHttpStatusMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints propios de Go4Clients: envío individual (motor SmsCore) y campañas (directo, sin feedback).
 */
@RestController
@RequestMapping("/v1/sms/go4clients")
@RequiredArgsConstructor
@Tag(name = "SMS Go4Clients", description = "Envío y campañas solo por Go4Clients")
public class Go4ClientsSmsController {

    private final Go4ClientsSendSmsUseCase sendSmsUseCase;
    private final Go4ClientsCampaignUseCase campaignUseCase;

    @Operation(summary = "Enviar SMS por Go4Clients",
            description = "Usa la ruta GO4CLIENTS del tipo de mensaje (3 intentos por cuenta) y queda en el log")
    @PostMapping("/send")
    public ResponseEntity<SmsEnvioResponse> send(
            @Valid @RequestBody SendSmsRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return SmsHttpStatusMapper.toResponse(sendSmsUseCase.send(request, idempotencyKey));
    }

    @Operation(summary = "Crear campaña", description = "Crea la campaña en Go4Clients. Sin reintentos ni log.")
    @PostMapping("/campaigns")
    public ResponseEntity<Go4ClientsCampaignResult> createCampaign(@Valid @RequestBody CreateGo4ClientsCampaignRequest request) {
        return toResponse(campaignUseCase.create(request));
    }

    @Operation(summary = "Enviar mensajes de una campaña", description = "Envía el mensaje a la lista de destinatarios de la campaña")
    @PostMapping("/campaigns/{campaignId}/messages")
    public ResponseEntity<Go4ClientsCampaignResult> sendMessages(
            @PathVariable String campaignId,
            @Valid @RequestBody SendGo4ClientsCampaignMessagesRequest request) {
        return toResponse(campaignUseCase.sendMessages(campaignId, request));
    }

    @Operation(summary = "Reporte de campaña", description = "Totales de entregados, fallidos y pendientes según Go4Clients")
    @GetMapping("/campaigns/{campaignId}/report")
    public ResponseEntity<Go4ClientsCampaignReport> report(
            @PathVariable String campaignId,
            @RequestParam(required = false) String cuenta) {
        Go4ClientsCampaignReport report = campaignUseCase.report(campaignId, cuenta);
        return ResponseEntity.status(report.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY).body(report);
    }

    private static ResponseEntity<Go4ClientsCampaignResult> toResponse(Go4ClientsCampaignResult result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        HttpStatus status = Go4ClientsCampaignResult.ERROR_SIN_CUENTA.equals(result.getErrorCode())
                ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(result);
    }
}
