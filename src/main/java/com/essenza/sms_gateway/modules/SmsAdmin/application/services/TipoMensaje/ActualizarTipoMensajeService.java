package com.essenza.sms_gateway.modules.SmsAdmin.application.services.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.ActualizarTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.TipoMensajeAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.TipoMensajeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActualizarTipoMensajeService implements ActualizarTipoMensajeUseCase {

    private final TipoMensajeAdminPort tipos;
    private final TipoMensajeMapper mapper;

    @Override
    public TipoMensajeResponse actualizar(Long id, TipoMensajeRequest request) {
        TipoMensaje tipo = tipos.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Tipo de mensaje no encontrado: " + id));
        mapper.updateModel(request, tipo);
        if (tipo.getOtp() == null) {
            tipo.setOtp(false);
        }
        if (tipo.getMasivo() == null) {
            tipo.setMasivo(false);
        }
        return mapper.toResponse(tipos.save(tipo));
    }
}
