package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;

import java.util.List;
import java.util.Optional;

public interface ProveedorAdminPort {
    List<Proveedor> findAll();
    Optional<Proveedor> findById(Long id);
    Optional<Proveedor> findByCodigo(String codigo);
    Proveedor save(Proveedor proveedor);
}
