package com.essenza.sms_gateway.modules.SmsCore.application.services;

import com.essenza.sms_gateway.modules.SmsCore.application.input.QuerySmsEnvioUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.RefreshSmsRoutingUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.PageResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioDetalleResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioFilter;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsEnvioQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta del log de envíos y administración de la caché de rutas.
 */
@Service
@RequiredArgsConstructor
public class SmsEnvioQueryService implements QuerySmsEnvioUseCase, RefreshSmsRoutingUseCase {

    private static final int MAX_PAGE_SIZE = 200;

    private final SmsEnvioQueryPort queryPort;
    private final RoutingPolicyPort routingPolicy;

    @Override
    public Optional<SmsEnvioDetalleResponse> findById(UUID envioId) {
        return queryPort.findById(envioId);
    }

    @Override
    public PageResult<SmsEnvioDetalleResponse> search(SmsEnvioFilter filter) {
        int size = Math.min(Math.max(1, filter.getSize()), MAX_PAGE_SIZE);
        int page = Math.max(0, filter.getPage());
        return queryPort.search(filter.toBuilder().page(page).size(size).build());
    }

    @Override
    public void refresh() {
        routingPolicy.refresh();
    }
}
