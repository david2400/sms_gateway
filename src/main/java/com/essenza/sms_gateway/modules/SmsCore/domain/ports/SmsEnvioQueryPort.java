package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.PageResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioDetalleResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioFilter;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta del log de envíos.
 */
public interface SmsEnvioQueryPort {

    Optional<SmsEnvioDetalleResponse> findById(UUID envioId);

    /** Búsqueda paginada; los intentos no se incluyen en la lista. */
    PageResult<SmsEnvioDetalleResponse> search(SmsEnvioFilter filter);
}
