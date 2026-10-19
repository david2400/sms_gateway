package com.essenza.sms_gateway.modules.SmsAdmin.application.services.support;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.SecretCheckPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.CuentaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Respuesta de cuenta: sin contraseña; indica si la variable de entorno existe y en qué rutas se usa. */
@Component
@RequiredArgsConstructor
public class CuentaResponseAssembler {

    private final CuentaMapper mapper;
    private final SecretCheckPort secretCheck;
    private final RutaPasoAdminPort pasos;

    public CuentaResponse toResponse(Cuenta cuenta) {
        return mapper.toResponse(cuenta,
                secretCheck.isDefined(cuenta.getSecretRef()),
                pasos.rutasQueUsanCuenta(cuenta.getId()));
    }
}
