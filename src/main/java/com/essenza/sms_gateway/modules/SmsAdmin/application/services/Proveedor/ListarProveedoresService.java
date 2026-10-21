package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ListarProveedoresUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.ProveedorResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListarProveedoresService implements ListarProveedoresUseCase {

    private final ProveedorAdminPort proveedores;
    private final ProveedorResponseAssembler assembler;

    @Override
    public List<ProveedorResponse> listar() {
        return proveedores.findAll().stream().map(assembler::toResponse).toList();
    }
}
