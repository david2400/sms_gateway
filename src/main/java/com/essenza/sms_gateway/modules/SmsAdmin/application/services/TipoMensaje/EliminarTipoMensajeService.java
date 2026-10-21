package com.essenza.sms_gateway.modules.SmsAdmin.application.services.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.EliminarTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.TipoMensajeAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EliminarTipoMensajeService implements EliminarTipoMensajeUseCase {

    private final TipoMensajeAdminPort tipos;
    private final RutaAdminPort rutas;

    @Override
    public void eliminar(Long id) {
        TipoMensaje tipo = tipos.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Tipo de mensaje no encontrado: " + id));
        if (!rutas.findAll(null, false, tipo.getCodigo()).isEmpty()) {
            throw new AdminConflictException(
                    "No se puede eliminar el tipo " + tipo.getCodigo() + ": existen rutas que lo usan");
        }
        tipos.delete(id);
    }
}
