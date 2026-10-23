package com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Respuesta de enviar mensajes: {@code { "generatedIds": [...] }}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Go4ClientsMessagesResponse {
    @JsonProperty("generatedIds")
    private List<String> generatedIds;
    @JsonIgnore
    private String rawBody;
}
