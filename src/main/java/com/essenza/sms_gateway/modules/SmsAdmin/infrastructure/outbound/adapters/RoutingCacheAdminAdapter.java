package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsCore.application.input.RefreshSmsRoutingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Descarta la caché de rutas del motor (SmsCore) DESPUÉS del commit, para que el próximo
 * envío lea la configuración nueva y no la anterior.
 */
@Component
@RequiredArgsConstructor
public class RoutingCacheAdminAdapter implements RoutingCacheAdminPort {

    private final RefreshSmsRoutingUseCase refreshRouting;

    @Override
    public void invalidarAlConfirmar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refreshRouting.refresh();
                }
            });
        } else {
            refreshRouting.refresh();
        }
    }
}
