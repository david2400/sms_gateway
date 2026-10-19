package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;

import java.util.List;
import java.util.Optional;

public interface CuentaAdminPort {
    /** @param proveedorId filtro opcional */
    List<Cuenta> findAll(Long proveedorId);
    Optional<Cuenta> findById(Long id);
    Optional<Cuenta> findByCodigo(String codigo);
    Cuenta save(Cuenta cuenta);
}
