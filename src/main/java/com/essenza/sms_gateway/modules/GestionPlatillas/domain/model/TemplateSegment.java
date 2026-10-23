package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

/**
 * Trozo de una plantilla ya parseada: texto literal o referencia a una variable.
 */
public record TemplateSegment(String literal, String variable) {

    public static TemplateSegment literal(String text) {
        return new TemplateSegment(text, null);
    }

    public static TemplateSegment variable(String name) {
        return new TemplateSegment(null, name);
    }

    public boolean isVariable() {
        return variable != null;
    }
}
