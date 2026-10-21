package com.essenza.sms_gateway.modules.SmsAdmin.application.services.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.ObtenerTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.TipoMensajeAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.TipoMensajeMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObtenerTipoMensajeService implements ObtenerTipoMensajeUseCase {

    private final TipoMensajeAdminPort tipos;
    private final TipoMensajeMapper mapper;

    @Override
    public TipoMensajeResponse obtener(Long id) {
        return tipos.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new AdminNotFoundException("Tipo de mensaje no encontrado: " + id));
    }

    @Override
    public TipoMensajeResponse obtenerPorCodigo(String codigo) {
        String normalizado = AdminCodigos.normalizar(codigo);
        return tipos.findByCodigo(normalizado).map(mapper::toResponse)
                .orElseThrow(() -> new AdminNotFoundException("Tipo de mensaje desconocido: " + normalizado));
    }
}
