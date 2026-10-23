package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.ResolvedContent;

import java.util.List;
import java.util.Map;

/**
 * SPI de plantillas: SmsCore NO conoce cómo se definen ni se validan las plantillas; lo implementa el
 * módulo {@code GestionPlatillas}. Si ningún módulo lo implementa, los envíos con plantilla responden 400.
 * <p>
 * Contrato de errores: lanzar {@link com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException}
 * ({@code PLANTILLA_NO_ENCONTRADA}, {@code VARIABLES_INVALIDAS}, {@code VALIDACION}, {@code PLANTILLA_MAL_CONFIGURADA}).
 */
public interface SmsTemplatePort {

    /**
     * Genera el texto de un envío.
     *
     * @param requestedType tipo pedido en el request; {@code null} = el de la plantilla
     */
    ResolvedContent resolve(String templateCode, Map<String, String> variables, MessageType requestedType);

    /**
     * Genera un texto por destinatario (variables globales + las del destinatario, gana la del destinatario).
     * Valida todos antes de devolver: si alguno falla, lanza la excepción con los errores de todos.
     */
    ResolvedContent resolveBatch(String templateCode, Map<String, String> globalVariables,
                                 List<Map<String, String>> perDestination, MessageType requestedType);

    /** Descarta cachés (lo invoca {@code POST /v1/sms/admin/routing-cache/refresh}). */
    default void refresh() {
    }
}
