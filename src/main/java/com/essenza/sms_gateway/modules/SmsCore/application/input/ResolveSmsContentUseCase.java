package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.ResolvedContent;

import java.util.List;
import java.util.Map;

/**
 * Construye el texto de un envío: mensaje directo o plantilla + variables (validación estricta).
 * Lo usan el endpoint central y los endpoints de cada proveedor.
 */
public interface ResolveSmsContentUseCase {

    /**
     * @param message       texto directo (excluyente con {@code templateCode})
     * @param templateCode  código de plantilla (excluyente con {@code message})
     * @param variables     valores de las variables de la plantilla
     * @param requestedType tipo pedido; si es {@code null} se usa el de la plantilla
     * @throws com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException si el request es inconsistente
     */
    ResolvedContent resolve(String message, String templateCode, Map<String, String> variables, MessageType requestedType);

    /**
     * Igual para un masivo: las variables de cada destinatario se combinan con las globales (gana la del destinatario).
     * Valida TODOS los destinatarios antes de enviar: si alguno falla no se envía ninguno.
     */
    ResolvedContent resolveBatch(String message, String templateCode, Map<String, String> globalVariables,
                                 List<Map<String, String>> perDestination, MessageType requestedType);
}
