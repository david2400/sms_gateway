package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.config.InalambriaProperties;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaWhatsAppMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InalambriaWhatsAppSenderTest {

    private final InalambriaWhatsAppSender sender = new InalambriaWhatsAppSender(
            Mappers.getMapper(InalambriaWhatsAppMapper.class), null, new InalambriaProperties());

    private static SmsDispatch wa() {
        return SmsDispatch.builder().to("3157132338").type(MessageType.WHATSAPP).externalTemplateId("ID_PLANTILLA")
                .templateParameter("Miguel").build();
    }

    @Test
    void validacionesPreviasSonPermanentes() {
        SendOutcome sinClave = sender.send(wa(), ProviderAccount.builder().code("C").username("u").build());
        assertFalse(sinClave.isRetryable());
        SendOutcome sinOrigen = sender.send(wa(), ProviderAccount.builder().code("C").username("u").password("p").build());
        assertFalse(sinOrigen.isRetryable());
        assertTrue(sinOrigen.getDetail().contains("sin número de origen"));
        SendOutcome sinPlantilla = sender.send(wa().toBuilder().externalTemplateId(null).build(),
                ProviderAccount.builder().code("C").username("u").password("p").sender("3000000000").build());
        assertFalse(sinPlantilla.isRetryable());
    }

    @Test
    void dispatchWhatsAppSeValidaPorPlantillaNoPorTexto() {
        assertTrue(wa().isValid());
        assertFalse(wa().toBuilder().externalTemplateId(" ").build().isValid());
        assertFalse(wa().toBuilder().templateParameter(" ").build().isValid());
    }
}
