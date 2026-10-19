package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Construye el payload de Inalambria (RESTAWS ver 3.1, endpoint MTMessage) a partir
 * del mensaje canónico de SmsCore.
 * <p>
 * Prioridad de los datos propios de Inalambria:
 * <ol>
 *   <li>{@code providerOptions} del request (endpoint propio de Inalambria)</li>
 *   <li>{@code sms_cuenta.parametros_json} de la cuenta</li>
 *   <li>valores por defecto (los mismos que usa el flujo legado)</li>
 * </ol>
 * Claves soportadas: {@code type}, {@code messagePattern}, {@code templateId}, {@code flashSms}, {@code url}.
 */
@Component
public class InalambriaDispatchPayloadMapper {

    public static final String OPT_TYPE = "type";
    public static final String OPT_MESSAGE_PATTERN = "messagePattern";
    public static final String OPT_TEMPLATE_ID = "templateId";
    public static final String OPT_FLASH_SMS = "flashSms";
    public static final String OPT_URL = "url";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HashMap<String, Object> toPayload(SmsDispatch message, ProviderAccount account) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("Type", asInt(value(OPT_TYPE, message, account), 1));
        payload.put("MessageText", message.getText());
        payload.put("Devices", normalizePhone(message.getTo(), message.getCountryCode()));
        payload.put("DateMessage", LocalDateTime.now().format(DATE_FORMAT));
        payload.put("FlashSMS", asBoolean(value(OPT_FLASH_SMS, message, account)) ? 1 : 0);
        payload.put("HasMore", 0);
        payload.put("MessagePattern", defaultIfBlank(value(OPT_MESSAGE_PATTERN, message, account), "0"));
        payload.put("TemplateId", asInt(value(OPT_TEMPLATE_ID, message, account), 0));
        payload.put("TransactionNumber", newTransactionNumber());
        payload.put("Url", defaultIfBlank(value(OPT_URL, message, account), ""));
        return payload;
    }

    /**
     * Inalambria exige el número con prefijo país y sin '+' (ej. 573157132338).
     */
    public String normalizePhone(String phone, String countryCode) {
        if (phone == null) {
            return "";
        }
        String digits = phone.trim().replace("+", "");
        if (digits.length() == 10) {
            return (countryCode != null && !countryCode.isBlank() ? countryCode : "57") + digits;
        }
        return digits;
    }

    /**
     * 28 dígitos: timestamp (13) + 15 aleatorios, para no repetir en el mismo milisegundo.
     */
    private String newTransactionNumber() {
        long random = ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L);
        return System.currentTimeMillis() + String.format("%015d", random);
    }

    private String value(String key, SmsDispatch message, ProviderAccount account) {
        String fromRequest = message.providerOption(key);
        if (fromRequest != null && !fromRequest.isBlank()) {
            return fromRequest;
        }
        return account.parameter(key);
    }

    private static int asInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean asBoolean(String value) {
        return value != null && ("1".equals(value.trim()) || "true".equalsIgnoreCase(value.trim()));
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
