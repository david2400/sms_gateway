package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.config.InalambriaProperties;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaSmsBidireccionalMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Una cuenta nunca se usa en el canal equivocado (error permanente → el motor pasa a la siguiente cuenta). */
class InalambriaSmsSenderCanalTest {

    private final InalambriaSmsSender sender = new InalambriaSmsSender(null, null, null,
            new InalambriaBidireccionalSender(Mappers.getMapper(InalambriaSmsBidireccionalMapper.class), null, new InalambriaProperties()));

    @Test
    void cuentaSmsNoEnviaWhatsApp() {
        SendOutcome r = sender.send(SmsDispatch.builder().to("3157132338").type(MessageType.WHATSAPP).externalTemplateId("T").build(),
                ProviderAccount.builder().code("INALAMBRIA_OTP").build());
        assertFalse(r.isRetryable());
        assertTrue(r.getDetail().contains("es de SMS"));
    }

    @Test
    void cuentaWhatsAppNoEnviaSms() {
        SendOutcome r = sender.send(SmsDispatch.builder().to("3157132338").type(MessageType.OTP).text("x").build(),
                ProviderAccount.builder().code("INALAMBRIA_WHATSAPP").parameter("canal", "whatsapp").build());
        assertFalse(r.isRetryable());
        assertTrue(r.getDetail().contains("es de WhatsApp"));
    }

    @Test
    void bidireccionalVaPorMomessageYExigeCodigoCorto() {
        SendOutcome r = sender.send(SmsDispatch.builder().to("3157132338").type(MessageType.BIDIRECCIONAL).text("Responde 1").build(),
                ProviderAccount.builder().code("INALAMBRIA_BIDIRECCIONAL").username("u").password("p").build());
        assertFalse(r.isRetryable());
        assertTrue(r.getDetail().contains("sin código corto"));
    }

    @Test
    void elMasivoNativoNoEnviaBidireccional() {
        InalambriaMassiveSmsSender masivo = new InalambriaMassiveSmsSender(null, null, new InalambriaProperties());
        assertTrue(masivo.supports(MessageType.MASIVO));
        assertFalse(masivo.supports(MessageType.BIDIRECCIONAL));
        assertFalse(masivo.supports(MessageType.WHATSAPP));
    }
}
