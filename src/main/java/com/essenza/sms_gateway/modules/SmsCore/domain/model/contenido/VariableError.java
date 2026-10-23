package com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido;

import lombok.Value;

/**
 * Error de validación de una variable (o de la sintaxis) de una plantilla.
 */
@Value
public class VariableError {

    public static final String FALTANTE = "FALTANTE";
    public static final String DESCONOCIDA = "DESCONOCIDA";
    public static final String TIPO_INVALIDO = "TIPO_INVALIDO";
    public static final String LONGITUD_EXCEDIDA = "LONGITUD_EXCEDIDA";
    public static final String FORMATO_INVALIDO = "FORMATO_INVALIDO";
    public static final String SINTAXIS = "SINTAXIS";
    public static final String MENSAJE_MUY_LARGO = "MENSAJE_MUY_LARGO";

    /** Nombre de la variable (con prefijo {@code destinations[i].} en masivos); {@code null} si es del mensaje. */
    String variable;
    String codigo;
    String mensaje;

    public VariableError withPrefix(String prefix) {
        return new VariableError(variable == null ? prefix : prefix + variable, codigo, mensaje);
    }
}
