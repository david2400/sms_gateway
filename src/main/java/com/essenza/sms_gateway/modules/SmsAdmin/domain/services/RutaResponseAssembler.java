package com.essenza.sms_gateway.modules.SmsAdmin.domain.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Arma la respuesta de ruta con sus pasos. */
@Service
@RequiredArgsConstructor
public class RutaResponseAssembler {

    private final RutaPasoAdminPort pasos;

    public RutaResponse toResponse(Ruta ruta) {
        List<RutaResponse.Paso> lista = pasos.findByRuta(ruta.getId()).stream()
                .map(p -> RutaResponse.Paso.builder()
                        .id(p.getId())
                        .orden(p.getOrden())
                        .cuentaId(p.getCuentaId())
                        .cuentaCodigo(p.getCuentaCodigo())
                        .proveedorCodigo(p.getProveedorCodigo())
                        .maxIntentos(p.getMaxIntentos())
                        .esperaMs(p.getEsperaMs())
                        .activo(p.isActivo())
                        .build())
                .toList();
        return RutaResponse.builder()
                .id(ruta.getId())
                .proveedorId(ruta.getProveedorId())
                .proveedorCodigo(nombre(ruta))
                .tipoMensaje(ruta.getTipoMensaje())
                .activo(ruta.isActivo())
                .pasos(lista)
                .build();
    }

    public static String nombre(Ruta ruta) {
        return ruta.isCentral() ? AdminCodigos.CENTRAL : ruta.getProveedorCodigo();
    }
}
