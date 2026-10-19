package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.ObtenerRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.RutaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager", readOnly = true)
public class ObtenerRutaService implements ObtenerRutaUseCase {

    private final AdminFinder finder;
    private final RutaResponseAssembler assembler;

    @Override
    public RutaResponse obtener(Long id) {
        return assembler.toResponse(finder.ruta(id));
    }
}
