package com.essenza.sms_gateway.modules.SmsCore.application.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.ResolvedContent;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsTemplatePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Reglas de SmsCore sobre el contenido; las plantillas se delegan al SPI (GestionPlatillas). */
class SmsContentResolverTest {

    /** SPI falso: devuelve el código recibido como texto. */
    private final SmsTemplatePort fake = new SmsTemplatePort() {
        @Override
        public ResolvedContent resolve(String code, Map<String, String> variables, MessageType type) {
            return new ResolvedContent(MessageType.NORMAL, code, List.of("render:" + code));
        }

        @Override
        public ResolvedContent resolveBatch(String code, Map<String, String> g, List<Map<String, String>> per, MessageType type) {
            return new ResolvedContent(MessageType.NORMAL, code, per.stream().map(x -> "render:" + code).toList());
        }
    };

    private final SmsContentResolver resolver = new SmsContentResolver(Optional.of(fake));

    @Test
    void textoDirectoExigeTipo() {
        ResolvedContent c = resolver.resolve("Hola", null, null, MessageType.NORMAL);
        assertEquals("Hola", c.text());
        assertNull(c.templateCode());
        assertThrows(SmsContenidoException.class, () -> resolver.resolve("Hola", null, null, null));
    }

    @Test
    void plantillaSeDelegaAlSpi() {
        assertEquals("render:SALUDO", resolver.resolve(null, " SALUDO ", Map.of("a", "b"), null).text());
        assertEquals(2, resolver.resolveBatch(null, "SALUDO", null, List.of(Map.of(), Map.of()), null).texts().size());
    }

    @Test
    void mensajeYPlantillaSonExcluyentes() {
        assertThrows(SmsContenidoException.class, () -> resolver.resolve("x", "SALUDO", Map.of(), null));
        assertThrows(SmsContenidoException.class, () -> resolver.resolve(null, null, null, MessageType.NORMAL));
        assertThrows(SmsContenidoException.class, () -> resolver.resolve("x", null, Map.of("a", "b"), MessageType.NORMAL));
        assertThrows(SmsContenidoException.class,
                () -> resolver.resolveBatch("x", null, null, List.of(Map.of("a", "b")), MessageType.NORMAL));
    }

    @Test
    void sinModuloDePlantillasResponde400() {
        SmsContentResolver sinModulo = new SmsContentResolver(Optional.empty());
        SmsContenidoException e = assertThrows(SmsContenidoException.class, () -> sinModulo.resolve(null, "SALUDO", Map.of(), null));
        assertEquals(SmsContenidoException.VALIDACION, e.getCodigo());
        assertEquals("x", sinModulo.resolve("x", null, null, MessageType.NORMAL).text());
    }
}
