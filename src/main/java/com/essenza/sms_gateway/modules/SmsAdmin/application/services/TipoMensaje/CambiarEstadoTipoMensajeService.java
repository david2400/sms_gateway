package com.essenza.sms_gateway.modules.SmsAdmin.application.services.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.CambiarEstadoTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.TipoMensajeAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.TipoMensajeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CambiarEstadoTipoMensajeService implements CambiarEstadoTipoMensajeUseCase {

    private final TipoMensajeAdminPort tipos;
    private final TipoMensajeMapper mapper;

    @Override
    public TipoMensajeResponse cambiarEstado(Long id, Boolean activo) {
        if (activo == null) {
            throw new AdminValidationException("activo es obligatorio");
        }
        TipoMensaje tipo = tipos.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Tipo de mensaje no encontrado: " + id));
        tipo.setActivo(activo);
        return mapper.toResponse(tipos.save(tipo));
    }
}
