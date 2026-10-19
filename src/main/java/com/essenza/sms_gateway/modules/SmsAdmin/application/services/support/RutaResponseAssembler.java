package com.essenza.sms_gateway.modules.SmsAdmin.application.services.support;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Respuesta de ruta con sus pasos; el proveedor de la ruta central se muestra como CENTRAL. */
@Component
@RequiredArgsConstructor
public class RutaResponseAssembler {

    private final RutaMapper mapper;
    private final RutaPasoAdminPort pasos;

    public RutaResponse toResponse(Ruta ruta) {
        return mapper.toResponse(ruta, nombre(ruta), pasos.findByRuta(ruta.getId()));
    }

    public static String nombre(Ruta ruta) {
        return ruta.isCentral() ? AdminCodigos.CENTRAL : ruta.getProveedorCodigo();
    }
}
