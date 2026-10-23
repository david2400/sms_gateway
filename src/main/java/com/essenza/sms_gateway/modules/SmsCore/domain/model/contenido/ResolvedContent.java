package com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;

import java.util.List;

/**
 * Contenido final de un envío: texto (directo o generado con plantilla) y tipo de mensaje efectivo.
 *
 * @param texts        un texto por destinatario (envío individual: un solo elemento)
 * @param templateCode plantilla usada o {@code null} si el texto vino directo
 */
public record ResolvedContent(MessageType type, String templateCode, List<String> texts) {

    public String text() {
        return texts.get(0);
    }
}
