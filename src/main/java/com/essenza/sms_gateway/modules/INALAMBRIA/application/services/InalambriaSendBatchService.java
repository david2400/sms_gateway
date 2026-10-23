package com.essenza.sms_gateway.modules.INALAMBRIA.application.services;

import com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms.InalambriaSendBatchUseCase;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria.InalambriaSmsSender;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsBatchUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsBatchRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsBatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Delega en el motor masivo de SmsCore con la ruta (INALAMBRIA, tipo). El motor usa
 * {@code InalambriaMassiveSmsSender} (lotes de {@code sms.inalambria.massive.chunk-size}).
 */
@Service
@RequiredArgsConstructor
public class InalambriaSendBatchService implements InalambriaSendBatchUseCase {

    private final SendSmsBatchUseCase sendSmsBatch;

    @Override
    public SmsBatchResponse send(SendSmsBatchRequest request) {
        return sendSmsBatch.send(request, InalambriaSmsSender.PROVIDER_CODE);
    }
}
