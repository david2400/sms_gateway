package com.essenza.sms_gateway.modules.GestionPlatillas.application.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.CompiledTemplate;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaQueryPort;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.services.TemplateRenderer;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.ResolvedContent;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.VariableError;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsTemplatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del SPI de plantillas de SmsCore ({@link SmsTemplatePort}): busca la plantilla activa,
 * decide el tipo de mensaje y genera el texto con validación estricta de variables.
 * <ul>
 *   <li>Tipo: el del request o, si no viene, el de la plantilla. Si ambos vienen deben coincidir.</li>
 *   <li>Masivo: variables globales + por destinatario; se reportan hasta {@value #MAX_ERRORS} errores con el
 *       prefijo {@code destinations[i].} y no se envía nada si un destinatario falla.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PlantillaContenidoService implements SmsTemplatePort {

    static final int MAX_ERRORS = 100;

    private final PlantillaQueryPort plantillas;
    private final TemplateRenderer renderer;

    @Override
    public ResolvedContent resolve(String templateCode, Map<String, String> variables, MessageType requestedType) {
        CompiledTemplate template = find(templateCode);
        MessageType type = typeOf(template, requestedType);
        return new ResolvedContent(type, template.getCode(), List.of(renderer.render(template, variables).getText()));
    }

    @Override
    public ResolvedContent resolveBatch(String templateCode, Map<String, String> globalVariables,
                                        List<Map<String, String>> perDestination, MessageType requestedType) {
        CompiledTemplate template = find(templateCode);
        MessageType type = typeOf(template, requestedType);
        int n = perDestination.size();

        List<String> texts = new ArrayList<>(n);
        List<VariableError> errors = new ArrayList<>();
        int failed = 0;
        for (int i = 0; i < n; i++) {
            Map<String, String> merged = new HashMap<>();
            if (globalVariables != null) {
                merged.putAll(globalVariables);
            }
            if (perDestination.get(i) != null) {
                merged.putAll(perDestination.get(i));
            }
            List<VariableError> own = new ArrayList<>();
            String text = renderer.tryRender(template, merged, own);
            if (text == null) {
                failed++;
                String prefix = "destinations[" + i + "].";
                for (VariableError e : own) {
                    if (errors.size() < MAX_ERRORS) {
                        errors.add(e.withPrefix(prefix));
                    }
                }
            }
            texts.add(text);
        }
        if (failed > 0) {
            throw new SmsContenidoException(SmsContenidoException.VARIABLES_INVALIDAS, template.getCode(),
                    failed + " de " + n + " destinatario(s) no cumplen la plantilla " + template.getCode()
                            + "; no se envió ningún SMS", errors);
        }
        return new ResolvedContent(type, template.getCode(), Collections.unmodifiableList(texts));
    }

    @Override
    public void refresh() {
        plantillas.refresh();
    }

    private CompiledTemplate find(String code) {
        return plantillas.findActive(code)
                .orElseThrow(() -> SmsContenidoException.noEncontrada(code.trim().toUpperCase()));
    }

    private static MessageType typeOf(CompiledTemplate template, MessageType requested) {
        if (requested == null && template.getMessageType() == null) {
            throw SmsContenidoException.validacion("La plantilla " + template.getCode()
                    + " no tiene tipo de mensaje por defecto: envíe 'tipo_mensaje'");
        }
        if (requested != null && template.getMessageType() != null && requested != template.getMessageType()) {
            throw SmsContenidoException.validacion("La plantilla " + template.getCode() + " es de tipo "
                    + template.getMessageType() + " y el request pide " + requested);
        }
        return requested != null ? requested : template.getMessageType();
    }
}
