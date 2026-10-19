package com.essenza.sms_gateway.modules.SmsAdmin.domain.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Una cuenta usada en la ruta propia de un proveedor no puede pasar a otro proveedor. */
@Service
@RequiredArgsConstructor
public class CambioProveedorCuentaValidator {

    private final RutaPasoAdminPort pasos;
    private final RutaAdminPort rutas;

    public void validar(Cuenta actual, Proveedor nuevo) {
        if (nuevo.getId().equals(actual.getProveedorId())) {
            return;
        }
        for (Long rutaId : pasos.rutasQueUsanCuenta(actual.getId())) {
            Ruta ruta = rutas.findById(rutaId).orElse(null);
            if (ruta != null && !ruta.isCentral() && !nuevo.getId().equals(ruta.getProveedorId())) {
                throw new AdminConflictException("La cuenta " + actual.getCodigo() + " está en la ruta " + rutaId
                        + " del proveedor " + ruta.getProveedorCodigo() + "; quítela de esa ruta antes de cambiarla de proveedor");
            }
        }
    }
}
