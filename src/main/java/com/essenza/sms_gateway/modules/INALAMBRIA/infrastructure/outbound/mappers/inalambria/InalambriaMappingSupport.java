package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Funciones comunes que usan las expresiones de los mappers MapStruct de Inalambria
 * ({@link InalambriaSmsMapper}, {@link InalambriaSmsMasivoMapper}, {@link InalambriaWhatsAppMapper}).
 * No es un mapper: es una utilidad sin estado (métodos estáticos).
 */
public final class InalambriaMappingSupport {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_DETAIL = 500;
    private static final String DEFAULT_COUNTRY = "57";

    /** Lectura tolerante: nombres sin importar mayúsculas, campos desconocidos ignorados, decimales exactos. */
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private InalambriaMappingSupport() {
    }

    // ------------------------------------------------------------------ request

    /** Inalambria exige el número con prefijo país y sin '+' (ej. 573157132338). */
    public static String normalizePhone(String phone, String countryCode) {
        if (phone == null) {
            return "";
        }
        String digits = phone.trim().replace("+", "");
        if (digits.length() == 10) {
            return (countryCode != null && !countryCode.isBlank() ? countryCode : DEFAULT_COUNTRY) + digits;
        }
        return digits;
    }

    /** Valor de una opción: primero el request ({@code providerOptions}), luego {@code parametros_json} de la cuenta. */
    public static String option(SmsDispatch message, ProviderAccount account, String key, String defaultValue) {
        String fromRequest = message.providerOption(key);
        if (fromRequest != null && !fromRequest.isBlank()) {
            return fromRequest;
        }
        String fromAccount = account.parameter(key);
        return fromAccount == null || fromAccount.isBlank() ? defaultValue : fromAccount;
    }

    public static int intOption(SmsDispatch message, ProviderAccount account, String key, int defaultValue) {
        String value = option(message, account, key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 1/true → 1, cualquier otro valor → 0. */
    public static int flagOption(SmsDispatch message, ProviderAccount account, String key) {
        return isTrue(option(message, account, key, null)) ? 1 : 0;
    }

    public static boolean isTrue(String value) {
        return value != null && ("1".equals(value.trim()) || "true".equalsIgnoreCase(value.trim()));
    }

    public static String now() {
        return LocalDateTime.now().format(DATE_FORMAT);
    }

    /** 28 dígitos: timestamp (13) + 15 aleatorios, para no repetir en el mismo milisegundo. */
    public static String newTransactionNumber() {
        long random = ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L);
        return System.currentTimeMillis() + String.format("%015d", random);
    }

    public static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /** Headers JSON + Basic con el usuario y la clave de la cuenta. */
    public static HttpHeaders basicAuthJson(ProviderAccount account) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(account.getUsername(), account.getPassword());
        return headers;
    }

    // ----------------------------------------------------------------- response

    /**
     * Convierte el body a la clase de respuesta; si no es JSON devuelve una instancia vacía. En ambos casos
     * guarda el body original con {@code setRawBody}.
     */
    public static <T> T parse(String body, Class<T> type, Supplier<T> empty, java.util.function.BiConsumer<T, String> rawSetter) {
        T result = null;
        if (body != null && !body.isBlank()) {
            try {
                result = JSON.readValue(body, type);
            } catch (Exception ignored) {
                // body no JSON: se conserva en rawBody
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

    /** 400 (payload), 401 (credenciales) y 403 (permisos/IP): reintentar con la misma cuenta no sirve. */
    public static boolean isPermanentHttp(int httpStatus) {
        return httpStatus == 400 || httpStatus == 401 || httpStatus == 403;
    }

    public static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DETAIL ? value : value.substring(0, MAX_DETAIL);
    }
}
