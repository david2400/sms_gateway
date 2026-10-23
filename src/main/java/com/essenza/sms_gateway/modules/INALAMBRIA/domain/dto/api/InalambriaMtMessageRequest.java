package com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body de {@code POST /MTMessage} (RESTAWS ver 3.1): SMS individual. Nombres exactos de la API (PascalCase).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"Type", "MessageText", "Devices", "DateMessage", "FlashSMS", "HasMore", "MessagePattern",
        "TemplateId", "TransactionNumber", "Url"})
public class InalambriaMtMessageRequest {
    /** 1 = masiva, 2 = personalizada, 3 = plantilla. */
    @JsonProperty("Type")
    private int type;
    @JsonProperty("MessageText")
    private String messageText;
    /** Destino con prefijo país y sin '+'. */
    @JsonProperty("Devices")
    private String devices;
    /** yyyy-MM-dd HH:mm:ss */
    @JsonProperty("DateMessage")
    private String dateMessage;
    /** 1 = Flash SMS. */
    @JsonProperty("FlashSMS")
    private int flashSms;
    @JsonProperty("HasMore")
    private int hasMore;
    @JsonProperty("MessagePattern")
    private String messagePattern;
    @JsonProperty("TemplateId")
    private int templateId;
    /** 28 dígitos, único por envío. */
    @JsonProperty("TransactionNumber")
    private String transactionNumber;
    @JsonProperty("Url")
    private String url;
}
