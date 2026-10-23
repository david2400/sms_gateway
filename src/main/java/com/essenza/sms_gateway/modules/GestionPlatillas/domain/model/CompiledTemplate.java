package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Plantilla lista para usar: contenido parseado una sola vez y patrones compilados.
 * Inmutable y segura entre hilos, se cachea por código.
 */
@Getter
public final class CompiledTemplate {

    private final Long id;
    private final String code;
    private final String name;
    /** Tipo de mensaje por defecto de la plantilla; {@code null} = lo define el request. */
    private final MessageType messageType;
    private final int version;
    private final String content;
    private final List<TemplateSegment> segments;
    /** Variables usadas en el contenido, en orden de aparición. */
    private final Set<String> usedVariables;
    /** Variables declaradas por nombre, en el orden configurado. */
    private final Map<String, TemplateVariable> variables;
    private final Map<String, Pattern> patterns;

    /**
     * @throws IllegalArgumentException si el contenido o los patrones son inválidos
     */
    public CompiledTemplate(Long id, String code, String name, MessageType messageType, int version, String content,
                            List<TemplateSegment> segments, Set<String> usedVariables, List<TemplateVariable> declared) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.messageType = messageType;
        this.version = version;
        this.content = content;
        this.segments = List.copyOf(segments);
        this.usedVariables = Collections.unmodifiableSet(new java.util.LinkedHashSet<>(usedVariables));
        Map<String, TemplateVariable> vars = new LinkedHashMap<>();
        Map<String, Pattern> pats = new LinkedHashMap<>();
        for (TemplateVariable v : declared) {
            vars.put(v.getName(), v);
            if (v.getPattern() != null && !v.getPattern().isBlank()) {
                try {
                    pats.put(v.getName(), Pattern.compile(v.getPattern()));
                } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException("Patrón inválido en la variable " + v.getName() + ": " + e.getDescription());
                }
            }
        }
        this.variables = Collections.unmodifiableMap(vars);
        this.patterns = Collections.unmodifiableMap(pats);
    }

    public Pattern patternOf(String variable) {
        return patterns.get(variable);
    }
}
