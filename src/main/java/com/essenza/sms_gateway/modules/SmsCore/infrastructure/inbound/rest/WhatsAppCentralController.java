package com.essenza.sms_gateway.modules.SmsCore.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsCore.application.input.SendWhatsAppUseCase;
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

/** Endpoint central de WhatsApp: ruta central del tipo WHATSAPP (todos los proveedores con ese servicio). */
@RestController
@RequestMapping("/v1/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Central", description = "Envío de WhatsApp (plantillas de Meta) orquestado entre proveedores")
public class WhatsAppCentralController {

    private final SendWhatsAppUseCase sendWhatsApp;

    @Operation(summary = "Enviar WhatsApp (central)",
            description = "Plantilla aprobada por Meta + parámetros en orden. Mismo motor del SMS: ruta central del tipo WHATSAPP, "
                    + "reintentos, fallback solo por proveedores con el servicio WHATSAPP y log (/v1/sms/envios).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aceptado por el proveedor"),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "422", description = "Sin ruta, cuenta no válida o el proveedor no tiene el servicio WHATSAPP"),
            @ApiResponse(responseCode = "502", description = "Todas las cuentas fallaron")
    })
    @PostMapping("/send")
    public ResponseEntity<SmsEnvioResponse> send(
            @Valid @RequestBody SendWhatsAppRequest request,
            @Parameter(description = "Llave para evitar envíos duplicados")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return SmsHttpStatusMapper.toResponse(sendWhatsApp.send(request, idempotencyKey, null));
    }
}
