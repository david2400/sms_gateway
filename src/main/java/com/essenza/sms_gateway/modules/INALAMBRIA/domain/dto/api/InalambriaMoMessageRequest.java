package com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body de {@code POST https://rest.inalambria.com/momessage}: SMS bidireccional desde un código corto. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"MessageText", "PhoneNumber", "ShortCode", "Url"})
public class InalambriaMoMessageRequest {
    @JsonProperty("MessageText")
    private String messageText;
    /** Destino con prefijo país y sin '+' (573XXXXXXXXX). */
    @JsonProperty("PhoneNumber")
    private String phoneNumber;
    /** Código corto asignado por Inalambria desde el que sale (y al que responde el usuario). */
    @JsonProperty("ShortCode")
    private String shortCode;
    /** URL larga que Inalambria acorta e inserta en el mensaje (opcional). */
    @JsonProperty("Url")
    private String url;
}
