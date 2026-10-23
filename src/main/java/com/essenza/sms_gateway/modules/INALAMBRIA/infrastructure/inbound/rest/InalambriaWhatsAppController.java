package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms.InalambriaSendWhatsAppUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendWhatsAppRequest;
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

/** WhatsApp solo por Inalambria (plantillas de Meta). */
@RestController
@RequestMapping("/v1/whatsapp/inalambria")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Inalambria", description = "Envío de WhatsApp con plantillas de Meta solo por Inalambria")
public class InalambriaWhatsAppController {

    private final InalambriaSendWhatsAppUseCase sendWhatsApp;

    @Operation(summary = "Enviar WhatsApp por Inalambria",
            description = "Ruta INALAMBRIA/WHATSAPP (cuenta INALAMBRIA_WHATSAPP). template_id = plantilla aprobada por Meta; "
                    + "parameters = valores en orden. El origen es el remitente de la cuenta salvo que se envíe 'source'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aceptado por Inalambria (transactionNumber generado)"),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "422", description = "Sin ruta o Inalambria sin el servicio WHATSAPP"),
            @ApiResponse(responseCode = "502", description = "Inalambria rechazó o no respondió")
    })
    @PostMapping("/send")
    public ResponseEntity<SmsEnvioResponse> send(
            @Valid @RequestBody SendWhatsAppRequest request,
            @Parameter(description = "Llave para evitar envíos duplicados")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return SmsHttpStatusMapper.toResponse(sendWhatsApp.send(request, idempotencyKey));
    }
}
