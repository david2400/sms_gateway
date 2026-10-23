package com.essenza.sms_gateway.modules.SmsCore.domain.exception;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.VariableError;
import lombok.Getter;

import java.util.List;

/**
 * El contenido del SMS no se pudo construir: request inconsistente, plantilla inexistente o variables inválidas.
 * Se lanza ANTES de registrar el envío y de gastar intentos.
 */
@Getter
public class SmsContenidoException extends RuntimeException {

    /** Request inconsistente (ej. mensaje y plantilla a la vez, falta tipo de mensaje). HTTP 400. */
    public static final String VALIDACION = "VALIDACION";
    /** La plantilla no existe o está inactiva. HTTP 422. */
    public static final String PLANTILLA_NO_ENCONTRADA = "PLANTILLA_NO_ENCONTRADA";
    /** Faltan variables, sobran o tienen valores inválidos. HTTP 400. */
    public static final String VARIABLES_INVALIDAS = "VARIABLES_INVALIDAS";
    /** La plantilla en BD tiene errores de configuración. HTTP 422. */
    public static final String PLANTILLA_MAL_CONFIGURADA = "PLANTILLA_MAL_CONFIGURADA";

    private final String codigo;
    private final String plantilla;
    private final List<VariableError> errores;

    public SmsContenidoException(String codigo, String plantilla, String message, List<VariableError> errores) {
        super(message);
        this.codigo = codigo;
        this.plantilla = plantilla;
        this.errores = errores == null ? List.of() : List.copyOf(errores);
    }

    public static SmsContenidoException validacion(String message) {
        return new SmsContenidoException(VALIDACION, null, message, List.of());
    }

    public static SmsContenidoException noEncontrada(String plantilla) {
        return new SmsContenidoException(PLANTILLA_NO_ENCONTRADA, plantilla,
                "La plantilla " + plantilla + " no existe o está inactiva", List.of());
    }

    public static SmsContenidoException variablesInvalidas(String plantilla, List<VariableError> errores) {
        return new SmsContenidoException(VARIABLES_INVALIDAS, plantilla,
                "Las variables no cumplen la plantilla " + plantilla + " (" + errores.size() + " error(es))", errores);
    }
}
