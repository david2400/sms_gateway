package com.essenza.sms_gateway.modules.SmsCore.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsCore.application.input.QuerySmsEnvioUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.RefreshSmsRoutingUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.PageResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioDetalleResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioFilter;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consulta del log de envíos y administración de rutas.
 */
@RestController
@RequestMapping("/v1/sms")
@RequiredArgsConstructor
@Tag(name = "SMS Log", description = "Log de envíos: con qué proveedor y cuenta se envió cada SMS")
public class SmsEnvioController {

    private final QuerySmsEnvioUseCase queryUseCase;
    private final RefreshSmsRoutingUseCase refreshUseCase;

    @Operation(summary = "Detalle de un envío", description = "Estado, proveedor/cuenta final e intentos")
    @GetMapping("/envios/{envioId}")
    public ResponseEntity<SmsEnvioDetalleResponse> findById(@PathVariable UUID envioId) {
        return queryUseCase.findById(envioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Buscar envíos", description = "Filtros opcionales; fechas en formato ISO (2026-10-19T00:00:00)")
    @GetMapping("/envios")
    public ResponseEntity<PageResult<SmsEnvioDetalleResponse>> search(
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) DispatchStatus estado,
            @RequestParam(name = "tipo_mensaje", required = false) MessageType tipoMensaje,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SmsEnvioFilter filter = SmsEnvioFilter.builder()
                .destino(destino).estado(estado).tipoMensaje(tipoMensaje).proveedor(proveedor)
                .desde(desde).hasta(hasta).page(page).size(size)
                .build();
        return ResponseEntity.ok(queryUseCase.search(filter));
    }

    @Operation(summary = "Recargar rutas", description = "Descarta la caché para tomar cambios hechos en sms_ruta / sms_ruta_paso / sms_cuenta")
    @PostMapping("/admin/routing-cache/refresh")
    public ResponseEntity<Void> refreshRouting() {
        refreshUseCase.refresh();
        return ResponseEntity.noContent().build();
    }
}
