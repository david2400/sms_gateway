package com.essenza.sms_gateway.modules.SmsAdmin.application.services.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.CrearTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.TipoMensajeAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.TipoMensajeMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CrearTipoMensajeService implements CrearTipoMensajeUseCase {

    private final TipoMensajeAdminPort tipos;
    private final TipoMensajeMapper mapper;

    @Override
    public TipoMensajeResponse crear(TipoMensajeRequest request) {
        TipoMensaje tipo = mapper.toModel(request);
        tipo.setCodigo(AdminCodigos.normalizar(request.getCodigo()));
        tipo.setOtp(Boolean.TRUE.equals(tipo.getOtp()));
        tipo.setMasivo(Boolean.TRUE.equals(tipo.getMasivo()));
        if (tipo.getActivo() == null) {
            tipo.setActivo(true);
        }
        tipos.findByCodigo(tipo.getCodigo()).ifPresent(t -> {
            throw new AdminConflictException("Ya existe el tipo de mensaje " + tipo.getCodigo());
        });
        return mapper.toResponse(tipos.save(tipo));
    }
}
