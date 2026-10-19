package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Filtros de consulta del log de envíos. Todos opcionales.
 */
@Value
@Builder(toBuilder = true)
public class SmsEnvioFilter {
    String destino;
    DispatchStatus estado;
    MessageType tipoMensaje;
    /** Código del proveedor final (ej. INALAMBRIA). */
    String proveedor;
    LocalDateTime desde;
    LocalDateTime hasta;
    @Builder.Default
    int page = 0;
    @Builder.Default
    int size = 20;
}
