package com.essenza.sms_gateway.modules.SmsCore.application.services;

import com.essenza.sms_gateway.modules.SmsCore.application.input.ResolveSmsContentUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.ResolvedContent;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsTemplatePort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reglas del contenido de un envío:
 * <ul>
 *   <li>Se envía {@code message} o {@code plantilla}, nunca ambos ni ninguno.</li>
 *   <li>Texto directo: {@code tipo_mensaje} obligatorio y sin {@code variables}.</li>
 *   <li>Plantilla: se delega en {@link SmsTemplatePort} (módulo {@code GestionPlatillas}).</li>
 * </ul>
 */
@Service
public class SmsContentResolver implements ResolveSmsContentUseCase {

    private final Optional<SmsTemplatePort> templates;

    public SmsContentResolver(Optional<SmsTemplatePort> templates) {
        this.templates = templates;
    }

    @Override
    public ResolvedContent resolve(String message, String templateCode, Map<String, String> variables, MessageType requestedType) {
        if (blank(templateCode)) {
            return direct(message, variables, requestedType, 1);
        }
        rejectBoth(message);
        return port().resolve(templateCode.trim(), variables, requestedType);
    }

    @Override
    public ResolvedContent resolveBatch(String message, String templateCode, Map<String, String> globalVariables,
                                        List<Map<String, String>> perDestination, MessageType requestedType) {
        if (blank(templateCode)) {
            if (perDestination.stream().anyMatch(v -> v != null && !v.isEmpty())) {
                throw SmsContenidoException.validacion("Las variables por destinatario solo aplican cuando se usa una plantilla");
            }
            return direct(message, globalVariables, requestedType, perDestination.size());
        }
        rejectBoth(message);
        return port().resolveBatch(templateCode.trim(), globalVariables, perDestination, requestedType);
    }

    private static ResolvedContent direct(String message, Map<String, String> variables, MessageType requestedType, int copies) {
        if (blank(message)) {
            throw SmsContenidoException.validacion("Debe enviar 'message' o 'plantilla'");
        }
        if (variables != null && !variables.isEmpty()) {
            throw SmsContenidoException.validacion("'variables' solo aplica cuando se usa una plantilla");
        }
        if (requestedType == null) {
            throw SmsContenidoException.validacion("'tipo_mensaje' es obligatorio cuando se envía 'message'");
        }
        return new ResolvedContent(requestedType, null, Collections.nCopies(copies, message));
    }

    private static void rejectBoth(String message) {
        if (!blank(message)) {
            throw SmsContenidoException.validacion("Envíe 'message' o 'plantilla', no ambos");
        }
    }

    private SmsTemplatePort port() {
        return templates.orElseThrow(() -> SmsContenidoException.validacion(
                "Las plantillas no están disponibles en esta instalación (módulo GestionPlatillas)"));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
