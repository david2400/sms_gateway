package com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Body de {@code POST /mtmessage/massive}: varios mensajes, cada uno con su texto y sus destinatarios.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InalambriaMasivoRequest {

    @JsonProperty("bulkId")
    private String bulkId;

    @JsonProperty("messages")
    private List<Mensaje> messages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Mensaje {
        @JsonProperty("destinations")
        private List<Destino> destinations;
        @JsonProperty("text")
        private String text;
        @JsonProperty("flash")
        private Boolean flash;
        @JsonProperty("notifyUrl")
        private String notifyUrl;
        @JsonProperty("notifyContentType")
        private String notifyContentType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Destino {
        /** Con prefijo país y sin '+'. */
        @JsonProperty("to")
        private String to;
        /** Id del envío en nuestro log; vuelve en los callbacks. */
        @JsonProperty("messageId")
        private String messageId;
    }
}
