package com.essenza.sms_gateway.modules.SmsAdmin.application.services.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.ListarTiposMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.TipoMensajeAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.TipoMensajeMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.CanalMensaje;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListarTiposMensajeService implements ListarTiposMensajeUseCase {

    private final TipoMensajeAdminPort tipos;
    private final TipoMensajeMapper mapper;

    @Override
    public List<TipoMensajeResponse> listar(CanalMensaje canal, Boolean soloOtp, Boolean soloMasivo) {
        return tipos.findAll(canal, soloOtp, soloMasivo).stream().map(mapper::toResponse).toList();
    }
}
