package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaCachePort;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Descarta la caché de plantillas DESPUÉS del commit, para que el siguiente envío vea la versión nueva. */
@Component
@RequiredArgsConstructor
public class PlantillaCacheAdapter implements PlantillaCachePort {

    private final PlantillaQueryPort plantillas;

    @Override
    public void invalidarAlConfirmar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    plantillas.refresh();
                }
            });
        } else {
            plantillas.refresh();
        }
    }
}
