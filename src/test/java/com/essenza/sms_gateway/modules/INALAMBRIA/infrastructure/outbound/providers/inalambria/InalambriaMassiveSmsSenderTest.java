package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.config.InalambriaProperties;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaMappingSupport;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InalambriaMassiveSmsSenderTest {

    @Test
    void clasificaErroresPermanentes() {
        assertTrue(InalambriaMappingSupport.isPermanentHttp(401));
        assertTrue(InalambriaMappingSupport.isPermanentHttp(400));
        assertFalse(InalambriaMappingSupport.isPermanentHttp(503));
        assertFalse(InalambriaMappingSupport.isPermanentHttp(429));
    }

    @Test
    void tamanoDeLotePorPropiedadOCuenta() {
        InalambriaProperties props = new InalambriaProperties();
        InalambriaMassiveSmsSender sender = new InalambriaMassiveSmsSender(null, null, props);
        assertEquals(500, sender.maxBatchSize(ProviderAccount.builder().code("C").build()));
        assertEquals(200, sender.maxBatchSize(ProviderAccount.builder().code("C").parameter("massiveChunkSize", "200").build()));
    }

    @Test
    void cuentaSinCredencialesEsPermanente() {
        InalambriaMassiveSmsSender sender = new InalambriaMassiveSmsSender(null, null, new InalambriaProperties());
        assertFalse(sender.sendBatch(java.util.List.of(), ProviderAccount.builder().code("C").build(), "L").isRetryable());
    }
}
