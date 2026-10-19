package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ObtenerProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.ProveedorResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager", readOnly = true)
public class ObtenerProveedorService implements ObtenerProveedorUseCase {

    private final AdminFinder finder;
    private final ProveedorResponseAssembler assembler;

    @Override
    public ProveedorResponse obtener(Long id) {
        return assembler.toResponse(finder.proveedor(id));
    }
}
