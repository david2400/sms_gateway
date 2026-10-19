package com.essenza.sms_gateway.modules.SmsAdmin.domain.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.SecretCheckPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Arma la respuesta de cuenta (sin contraseña; indica si la variable de entorno existe). */
@Service
@RequiredArgsConstructor
public class CuentaResponseAssembler {

    private final SecretCheckPort secretCheck;
    private final RutaPasoAdminPort pasos;

    public CuentaResponse toResponse(Cuenta c) {
        return CuentaResponse.builder()
                .id(c.getId())
                .proveedorId(c.getProveedorId())
                .proveedorCodigo(c.getProveedorCodigo())
                .codigo(c.getCodigo())
                .usuario(c.getUsuario())
                .secretRef(c.getSecretRef())
                .secretDefinido(secretCheck.isDefined(c.getSecretRef()))
                .remitente(c.getRemitente())
                .parametros(c.getParametros())
                .activo(c.isActivo())
                .rutas(pasos.rutasQueUsanCuenta(c.getId()))
                .build();
    }
}
