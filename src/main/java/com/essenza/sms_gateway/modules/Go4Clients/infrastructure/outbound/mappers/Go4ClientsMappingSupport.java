package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Funciones comunes que usan las expresiones de los mappers MapStruct de Go4Clients
 * ({@link Go4ClientsCampaignMapper}, {@link Go4ClientsMessageMapper}, {@link Go4ClientsReportMapper}).
 * No es un mapper: utilidad sin estado (métodos estáticos).
 */
public final class Go4ClientsMappingSupport {

    public static final String DEFAULT_SENDER = "RANDOM";
    public static final String DEFAULT_PRIORITY = "HIGH";
    public static final String DEFAULT_COUNTRY = "57";
    private static final int MAX_DETAIL = 500;

    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private Go4ClientsMappingSupport() {
    }

    /** apiKey = {@code SmsCuenta.usuario} (o {@code env:...}); apiSecret = clave cifrada o {@code secret_ref}. */
    public static HttpHeaders headers(ProviderAccount account) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apiKey", account.getUsername());
        headers.set("apiSecret", account.getPassword());
        return headers;
    }

    public static String baseUrl(ProviderAccount account) {
        String url = account.getBaseUrl();
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Go4Clients recibe el número nacional y el código de país por separado. */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.trim().replace("+", "");
        if (digits.length() == 12 && digits.startsWith(DEFAULT_COUNTRY)) {
            return digits.substring(2);
        }
        return digits;
    }

    public static String orDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    /** Remitente: el pedido, si no el de la cuenta, si no {@code parametros_json.sender}, si no RANDOM. */
    public static String resolveSender(ProviderAccount account, String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        if (account.getSender() != null && !account.getSender().isBlank()) {
            return account.getSender();
        }
        return orDefault(account.parameter("sender"), DEFAULT_SENDER);
    }

    /** Body a la clase de respuesta (o instancia vacía si no es JSON), conservando el body original. */
    public static <T> T parse(String body, Class<T> type, Supplier<T> empty, BiConsumer<T, String> rawSetter) {
        T result = null;
        if (body != null && !body.isBlank()) {
            try {
                result = JSON.readValue(body, type);
            } catch (Exception ignored) {
                // body no JSON: queda en rawBody
            }
        }
        if (result == null) {
            result = empty.get();
        }
        rawSetter.accept(result, body);
        return result;
    }

    public static boolean is2xx(int httpStatus) {
        return httpStatus >= 200 && httpStatus < 300;
    }

    public static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DETAIL ? value : value.substring(0, MAX_DETAIL);
    }
}
