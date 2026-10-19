package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.ListarRutasUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.RutaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager", readOnly = true)
public class ListarRutasService implements ListarRutasUseCase {

    private final RutaAdminPort rutas;
    private final RutaResponseAssembler assembler;

    @Override
    public List<RutaResponse> listar(Long proveedorId, boolean soloCentral, MessageType tipoMensaje) {
        return rutas.findAll(proveedorId, soloCentral, tipoMensaje != null ? tipoMensaje.name() : null)
                .stream().map(assembler::toResponse).toList();
    }
}
