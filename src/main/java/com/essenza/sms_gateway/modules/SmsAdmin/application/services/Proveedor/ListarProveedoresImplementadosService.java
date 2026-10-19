package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ListarProveedoresImplementadosUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorCatalogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListarProveedoresImplementadosService implements ListarProveedoresImplementadosUseCase {

    private final ProveedorCatalogPort catalog;

    @Override
    public Set<String> implementados() {
        return catalog.implementados();
    }
}
