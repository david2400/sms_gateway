package com.essenza.sms_gateway.modules.SmsCore.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsBatchUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsBatchRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsBatchResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.shared.SmsHttpStatusMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint centralizado: orquesta TODOS los proveedores según la ruta central en BD
 * (3 intentos por cuenta y luego la siguiente cuenta/proveedor).
 */
@Slf4j
@RestController
@RequestMapping("/v1/sms")
@RequiredArgsConstructor
@Tag(name = "SMS Central", description = "Envío de SMS orquestado entre todos los proveedores")
public class SmsCentralController {

    private final SendSmsUseCase sendSmsUseCase;
    private final SendSmsBatchUseCase sendSmsBatchUseCase;

    @Operation(summary = "Enviar SMS (central)",
            description = "Elige cuenta y proveedor según la ruta central configurada en BD para el tipo de mensaje. "
                    + "Hace hasta N intentos por cuenta (3 por defecto) y luego pasa a la siguiente. "
                    + "Texto directo ('message') o plantilla ('plantilla' + 'variables', validación estricta). "
                    + "Opcionales: 'proveedor' (preferido) y 'cuenta': van primero y el resto de la ruta queda de respaldo. "
                    + "El fallback solo usa proveedores que tienen el servicio del tipo de mensaje.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMS enviado",
                    content = @Content(schema = @Schema(implementation = SmsEnvioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido o variables de plantilla inválidas (lista de errores)"),
            @ApiResponse(responseCode = "422", description = "Sin ruta, cuenta no válida, plantilla inexistente o el proveedor no "
                    + "cuenta con el servicio (SERVICIO_NO_SOPORTADO)"),
            @ApiResponse(responseCode = "502", description = "Todas las cuentas/proveedores fallaron")
    })
    @PostMapping("/send")
    public ResponseEntity<SmsEnvioResponse> send(
            @Valid @RequestBody SendSmsRequest request,
            @Parameter(description = "Llave para evitar envíos duplicados si el cliente repite el request")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        log.info("[SMS-CENTRAL] Envío tipo {} plantilla {} a ****{}", request.getTipoMensaje(), request.getPlantilla(),
                lastDigits(request.getTo()));
        DispatchResult result = sendSmsUseCase.send(request, idempotencyKey);
        return SmsHttpStatusMapper.toResponse(result);
    }

    @Operation(summary = "Enviar SMS masivo (central)",
            description = "Hasta 5000 destinatarios. Texto igual ('message') o personalizado ('plantilla' + variables globales y "
                    + "por destinatario; si alguno es inválido no se envía ninguno). Cada destinatario queda en el log; los "
                    + "proveedores con API masiva (Inalambria) reciben lotes. Responde 200 con el resultado de cada destinatario.")
    @PostMapping("/batch")
    public ResponseEntity<SmsBatchResponse> sendBatch(@Valid @RequestBody SendSmsBatchRequest request) {
        log.info("[SMS-CENTRAL] Envío masivo tipo {} plantilla {} a {} destinatarios", request.getTipoMensaje(),
                request.getPlantilla(), request.getDestinations().size());
        return ResponseEntity.ok(sendSmsBatchUseCase.send(request));
    }

    private static String lastDigits(String phone) {
        return phone != null && phone.length() >= 4 ? phone.substring(phone.length() - 4) : "";
    }
}
