package com.essenza.sms_gateway.modules.GestionPlatillas.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.CompiledTemplate;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TemplateSegment;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TemplateVariable;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.VariableError;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parser de plantillas SMS. Sintaxis: {@code {{nombre}}} (se permiten espacios: {@code {{ nombre }}}).
 * <ul>
 *   <li>Nombre: empieza por letra; letras, números y {@code _}; máximo 50 caracteres.</li>
 *   <li>{@code {{} sin cierre, nombres vacíos o inválidos son errores de sintaxis.</li>
 *   <li>Para escribir llaves literales use {@code \{{} (la barra se elimina).</li>
 * </ul>
 * Utilidad pura (sin estado): la usan el motor de plantillas al cargarlas y la administración al validarlas.
 */
public final class TemplateParser {

    public static final Pattern VARIABLE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,49}");
    private static final String OPEN = "{{";
    private static final String CLOSE = "}}";

    private TemplateParser() {
    }

    /** Resultado del parseo. */
    public record Parsed(List<TemplateSegment> segments, Set<String> variables, List<VariableError> errors) {
        public boolean isValid() {
            return errors.isEmpty();
        }
    }

    public static Parsed parse(String content) {
        List<TemplateSegment> segments = new ArrayList<>();
        Set<String> variables = new LinkedHashSet<>();
        List<VariableError> errors = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            errors.add(new VariableError(null, VariableError.SINTAXIS, "El contenido de la plantilla está vacío"));
            return new Parsed(segments, variables, errors);
        }

        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < content.length()) {
            if (content.startsWith("\\" + OPEN, i)) {
                literal.append(OPEN);
                i += 3;
                continue;
            }
            if (content.startsWith(OPEN, i)) {
                int end = content.indexOf(CLOSE, i + 2);
                if (end < 0) {
                    errors.add(new VariableError(null, VariableError.SINTAXIS,
                            "'{{' sin cerrar en la posición " + (i + 1)));
                    literal.append(content, i, content.length());
                    break;
                }
                String name = content.substring(i + 2, end).trim();
                if (!VARIABLE_NAME.matcher(name).matches()) {
                    errors.add(new VariableError(name.isEmpty() ? null : name, VariableError.SINTAXIS,
                            "Nombre de variable inválido '" + name + "' en la posición " + (i + 1)
                                    + ": use letras, números y _ (debe empezar por letra)"));
                } else {
                    if (!literal.isEmpty()) {
                        segments.add(TemplateSegment.literal(literal.toString()));
                        literal.setLength(0);
                    }
                    segments.add(TemplateSegment.variable(name));
                    variables.add(name);
                }
                i = end + 2;
                continue;
            }
            literal.append(content.charAt(i));
            i++;
        }
        if (!literal.isEmpty()) {
            segments.add(TemplateSegment.literal(literal.toString()));
        }
        return new Parsed(segments, variables, errors);
    }

    /**
     * Parsea y compila una plantilla.
     *
     * @throws IllegalArgumentException si la sintaxis o los patrones son inválidos, o si el contenido usa
     *                                  variables no declaradas
     */
    public static CompiledTemplate compile(Long id, String code, String name, MessageType type, int version,
                                           String content, List<TemplateVariable> declared) {
        Parsed parsed = parse(content);
        if (!parsed.isValid()) {
            throw new IllegalArgumentException(parsed.errors().get(0).getMensaje());
        }
        Set<String> declaredNames = new LinkedHashSet<>();
        declared.forEach(v -> declaredNames.add(v.getName()));
        for (String used : parsed.variables()) {
            if (!declaredNames.contains(used)) {
                throw new IllegalArgumentException("La variable {{" + used + "}} se usa en el contenido pero no está declarada");
            }
        }
        return new CompiledTemplate(id, code, name, type, version, content, parsed.segments(), parsed.variables(), declared);
    }
}
