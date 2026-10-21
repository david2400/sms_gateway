package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.ListarCuentasUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.CuentaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListarCuentasService implements ListarCuentasUseCase {

    private final CuentaAdminPort cuentas;
    private final CuentaResponseAssembler assembler;

    @Override
    public List<CuentaResponse> listar(Long proveedorId) {
        return cuentas.findAll(proveedorId).stream().map(assembler::toResponse).toList();
    }
}
