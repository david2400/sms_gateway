package com.essenza.sms_gateway.modules.GestionPlatillas.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.CompiledTemplate;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.RenderedSms;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.SmsEncoding;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TemplateSegment;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TemplateVariable;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.VariableError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Aplica las variables del request a una plantilla compilada, en modo ESTRICTO:
 * <ul>
 *   <li>Variable obligatoria sin valor (y sin valor por defecto) → {@code FALTANTE}.</li>
 *   <li>Variable enviada que la plantilla no declara → {@code DESCONOCIDA}.</li>
 *   <li>Valor que no cumple el tipo, la longitud o el patrón → {@code TIPO_INVALIDO}, {@code LONGITUD_EXCEDIDA}, {@code FORMATO_INVALIDO}.</li>
 *   <li>Texto final de más de {@value #MAX_LENGTH} caracteres → {@code MENSAJE_MUY_LARGO}.</li>
 * </ul>
 * Reporta TODOS los errores juntos (no solo el primero) para que el cliente los corrija en un solo intento.
 * Sin estado: segura entre hilos.
 */
@Component
public class TemplateRenderer {

    public static final int MAX_LENGTH = 1600;

    /**
     * @throws SmsContenidoException {@code VARIABLES_INVALIDAS} con la lista de errores
     */
    public RenderedSms render(CompiledTemplate template, Map<String, String> values) {
        List<VariableError> errors = new ArrayList<>();
        String text = tryRender(template, values, errors);
        if (!errors.isEmpty()) {
            throw SmsContenidoException.variablesInvalidas(template.getCode(), errors);
        }
        return describe(template, text);
    }

    /**
     * Igual que {@link #render} pero acumula los errores en {@code errors} y devuelve {@code null} si hubo alguno.
     * Útil en masivos para reportar los errores de todos los destinatarios a la vez.
     */
    public String tryRender(CompiledTemplate template, Map<String, String> values, List<VariableError> errors) {
        Map<String, String> input = values != null ? values : Map.of();
        int before = errors.size();

        for (String key : input.keySet()) {
            if (!template.getVariables().containsKey(key)) {
                errors.add(new VariableError(key, VariableError.DESCONOCIDA,
                        "La plantilla " + template.getCode() + " no usa la variable '" + key + "'"));
            }
        }

        Map<String, String> formatted = new HashMap<>();
        for (TemplateVariable v : template.getVariables().values()) {
            String raw = input.get(v.getName());
            if (raw == null || raw.isBlank()) {
                if (v.getDefaultValue() != null && !v.getDefaultValue().isBlank()) {
                    raw = v.getDefaultValue();
                } else if (v.isRequired()) {
                    errors.add(new VariableError(v.getName(), VariableError.FALTANTE,
                            "La variable '" + v.getName() + "' es obligatoria"));
                    continue;
                } else {
                    formatted.put(v.getName(), "");
                    continue;
                }
            }
            if (v.getMaxLength() != null && raw.trim().length() > v.getMaxLength()) {
                errors.add(new VariableError(v.getName(), VariableError.LONGITUD_EXCEDIDA,
                        "La variable '" + v.getName() + "' admite máximo " + v.getMaxLength() + " caracteres"));
                continue;
            }
            Pattern pattern = template.patternOf(v.getName());
            if (pattern != null && !pattern.matcher(raw.trim()).matches()) {
                errors.add(new VariableError(v.getName(), VariableError.FORMATO_INVALIDO,
                        "La variable '" + v.getName() + "' no cumple el formato " + v.getPattern()));
                continue;
            }
            try {
                formatted.put(v.getName(), v.getDataType().format(raw));
            } catch (IllegalArgumentException e) {
                errors.add(new VariableError(v.getName(), VariableError.TIPO_INVALIDO,
                        "La variable '" + v.getName() + "' " + e.getMessage()));
            }
        }
        if (errors.size() > before) {
            return null;
        }

        StringBuilder sb = new StringBuilder(template.getContent().length() + 64);
        for (TemplateSegment s : template.getSegments()) {
            sb.append(s.isVariable() ? formatted.getOrDefault(s.variable(), "") : s.literal());
        }
        String text = sb.toString();
        if (text.length() > MAX_LENGTH) {
            errors.add(new VariableError(null, VariableError.MENSAJE_MUY_LARGO,
                    "El mensaje generado tiene " + text.length() + " caracteres (máximo " + MAX_LENGTH + ")"));
            return null;
        }
        if (text.isBlank()) {
            errors.add(new VariableError(null, VariableError.FALTANTE, "El mensaje generado quedó vacío"));
            return null;
        }
        return text;
    }

    public RenderedSms describe(CompiledTemplate template, String text) {
        SmsEncoding encoding = SmsSegmentCalculator.encodingOf(text);
        int length = SmsSegmentCalculator.lengthOf(text, encoding);
        return new RenderedSms(text, template.getCode(), template.getVersion(), length, encoding,
                SmsSegmentCalculator.partsOf(length, encoding));
    }
}
