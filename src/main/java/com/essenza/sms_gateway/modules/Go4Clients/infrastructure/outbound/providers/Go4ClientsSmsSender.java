package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.providers;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.Go4ClientsDestinatarioDto;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.Go4Clients.domain.ports.Go4ClientsCampaignPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Go4Clients como proveedor del motor de SmsCore (envío individual).
 * <p>
 * Go4Clients exige una campaña para enviar, así que cada intento crea una campaña
 * {@code SMS-<envioId>} y envía el mensaje al único destinatario. Si cualquiera de las
 * dos llamadas falla, el intento cuenta como fallido con el HTTP status de esa llamada.
 */
@Component
@RequiredArgsConstructor
public class Go4ClientsSmsSender implements SmsSenderPort {

    public static final String PROVIDER_CODE = "GO4CLIENTS";

    private final Go4ClientsCampaignPort campaignPort;

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SendOutcome send(SmsDispatch message, ProviderAccount account) {
        long start = System.currentTimeMillis();

        Go4ClientsCampaignResult campaign = campaignPort.createCampaign(account, "SMS-" + message.getId(), message.getSender());
        if (!campaign.isSuccess() || campaign.getCampaignId() == null) {
            return SendOutcome.fail(campaign.getHttpStatus(),
                    "Crear campaña: " + (campaign.getErrorMessage() != null ? campaign.getErrorMessage() : "sin id de campaña"),
                    System.currentTimeMillis() - start);
        }

        Go4ClientsDestinatarioDto destino = Go4ClientsDestinatarioDto.builder()
                .phoneNumber(message.getTo())
                .countryCode(message.getCountryCode())
                .build();
        Go4ClientsCampaignResult sent = campaignPort.sendMessages(account, campaign.getCampaignId(), message.getText(),
                List.of(destino), message.providerOption("priority"), null);

        long elapsed = System.currentTimeMillis() - start;
        return sent.isSuccess()
                ? SendOutcome.ok(sent.getHttpStatus(), elapsed)
                : SendOutcome.fail(sent.getHttpStatus(), "Enviar mensaje: " + sent.getErrorMessage(), elapsed);
    }
}
