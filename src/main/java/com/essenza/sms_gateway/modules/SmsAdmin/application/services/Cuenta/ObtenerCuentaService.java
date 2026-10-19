package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.ObtenerCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.CuentaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager", readOnly = true)
public class ObtenerCuentaService implements ObtenerCuentaUseCase {

    private final AdminFinder finder;
    private final CuentaResponseAssembler assembler;

    @Override
    public CuentaResponse obtener(Long id) {
        return assembler.toResponse(finder.cuenta(id));
    }
}
