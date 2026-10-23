package com.essenza.sms_gateway.modules.SmsAdmin.application.services.ProveedorServicio;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.ProveedorServicio.ListarServiciosProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ServiciosProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorServicioAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListarServiciosProveedorService implements ListarServiciosProveedorUseCase {

    private final AdminFinder finder;
    private final ProveedorServicioAdminPort servicios;

    @Override
    public ServiciosProveedorResponse listar(Long proveedorId) {
        Proveedor proveedor = finder.proveedor(proveedorId);
        return new ServiciosProveedorResponse(proveedor.getId(), proveedor.getCodigo(),
                servicios.tiposDe(proveedorId).stream().map(TipoMensaje::getCodigo).toList());
    }
}
