package com.essenza.sms_gateway.modules.SmsAdmin.domain.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Búsquedas que fallan con el error correcto: 404 cuando el recurso del path no existe,
 * 400 cuando el id viene en el body (referencia inválida).
 */
@Service
@RequiredArgsConstructor
public class AdminFinder {

    private final ProveedorAdminPort proveedores;
    private final CuentaAdminPort cuentas;
    private final RutaAdminPort rutas;

    public Proveedor proveedor(Long id) {
        return proveedores.findById(id).orElseThrow(() -> new AdminNotFoundException("No existe el proveedor " + id));
    }

    public Proveedor proveedorReferenciado(Long id) {
        return proveedores.findById(id).orElseThrow(() -> new AdminValidationException("No existe el proveedor " + id));
    }

    public Cuenta cuenta(Long id) {
        return cuentas.findById(id).orElseThrow(() -> new AdminNotFoundException("No existe la cuenta " + id));
    }

    public Cuenta cuentaReferenciada(Long id) {
        return cuentas.findById(id).orElseThrow(() -> new AdminValidationException("No existe la cuenta " + id));
    }

    public Ruta ruta(Long id) {
        return rutas.findById(id).orElseThrow(() -> new AdminNotFoundException("No existe la ruta " + id));
    }
}
