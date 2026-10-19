package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.PageResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioDetalleResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioFilter;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta del log de envíos de SMS.
 */
public interface QuerySmsEnvioUseCase {

    Optional<SmsEnvioDetalleResponse> findById(UUID envioId);

    PageResult<SmsEnvioDetalleResponse> search(SmsEnvioFilter filter);
}
