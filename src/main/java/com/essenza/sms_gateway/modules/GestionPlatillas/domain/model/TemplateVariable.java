package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Variable declarada en una plantilla ({@code SmsPlantillaVariable}).
 */
@Value
@Builder
public class TemplateVariable {

    /** Nombre usado en el contenido como {@code {{nombre}}}. */
    String name;
    @Builder.Default
    VariableDataType dataType = VariableDataType.TEXTO;
    @Builder.Default
    boolean required = true;
    /** Longitud máxima del valor recibido; {@code null} = sin límite propio. */
    Integer maxLength;
    /** Expresión regular que debe cumplir el valor completo; {@code null} = sin restricción. */
    String pattern;
    /** Valor usado cuando el request no envía la variable. */
    String defaultValue;
    String description;
}
