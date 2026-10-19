package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.Go4ClientsDestinatarioDto;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payloads, headers y lectura de respuestas de la API de campañas de Go4Clients
 * ({@code /campaigns/sms/v1.0} y {@code /analytics/sms/v1.0}).
 * Se usan mapas con claves explícitas para no depender de la estrategia de nombres de Jackson.
 */
@Component
public class Go4ClientsPayloadMapper {

    public static final String CAMPAIGNS_PATH = "/campaigns/sms/v1.0";
    public static final String ANALYTICS_PATH = "/analytics/sms/v1.0";
    private static final String DEFAULT_SENDER = "RANDOM";
    private static final String DEFAULT_PRIORITY = "HIGH";

    /** apiKey = {@code sms_cuenta.usuario} (o {@code env:...}); apiSecret = valor de {@code secret_ref}. */
    public HttpHeaders headers(ProviderAccount account) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apiKey", account.getUsername());
        headers.set("apiSecret", account.getPassword());
        return headers;
    }

    public Map<String, Object> createCampaignPayload(ProviderAccount account, String name, String sender) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("sender", resolveSender(account, sender));
        return payload;
    }

    public Map<String, Object> sendMessagesPayload(String message, List<Go4ClientsDestinatarioDto> destinations,
                                                   String priority, Map<String, Map<String, String>> landingCustomFields) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("priority", priority != null && !priority.isBlank() ? priority : DEFAULT_PRIORITY);
        payload.put("message", message);
        List<Map<String, String>> list = new ArrayList<>();
        for (Go4ClientsDestinatarioDto d : destinations) {
            Map<String, String> item = new HashMap<>();
            item.put("phoneNumber", normalizePhone(d.getPhoneNumber()));
            item.put("countryCode", d.getCountryCode() != null && !d.getCountryCode().isBlank() ? d.getCountryCode() : "57");
            list.add(item);
        }
        payload.put("destinationsList", list);
        if (landingCustomFields != null && !landingCustomFields.isEmpty()) {
            payload.put("landingCustomFields", landingCustomFields);
        }
        return payload;
    }

    /**
     * Go4Clients recibe el número nacional y el código de país por separado.
     */
    public String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.trim().replace("+", "");
        if (digits.length() == 12 && digits.startsWith("57")) {
            return digits.substring(2);
        }
        return digits;
    }

    public String resolveSender(ProviderAccount account, String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        if (account.getSender() != null && !account.getSender().isBlank()) {
            return account.getSender();
        }
        String fromParams = account.parameter("sender");
        return fromParams != null && !fromParams.isBlank() ? fromParams : DEFAULT_SENDER;
    }

    public String campaignsUrl(ProviderAccount account) {
        return trimSlash(account.getBaseUrl()) + CAMPAIGNS_PATH;
    }

    public String analyticsUrl(ProviderAccount account, String campaignId) {
        return trimSlash(account.getBaseUrl()) + ANALYTICS_PATH + "?uniqueCampaignID=" + campaignId;
    }

    public String text(Map<?, ?> body, String key) {
        Object v = body != null ? body.get(key) : null;
        return v != null ? v.toString() : null;
    }

    public int number(Map<?, ?> body, String key) {
        Object v = body != null ? body.get(key) : null;
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return v != null ? Integer.parseInt(v.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int listSize(Map<?, ?> body, String key) {
        Object v = body != null ? body.get(key) : null;
        return v instanceof List<?> l ? l.size() : 0;
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
