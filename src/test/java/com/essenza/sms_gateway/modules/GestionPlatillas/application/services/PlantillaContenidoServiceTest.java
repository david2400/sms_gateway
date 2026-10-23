package com.essenza.sms_gateway.modules.GestionPlatillas.application.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.CompiledTemplate;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TemplateVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaQueryPort;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.services.TemplateParser;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.services.TemplateRenderer;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.ResolvedContent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantillaContenidoServiceTest {

    private static final CompiledTemplate SALUDO = TemplateParser.compile(1L, "SALUDO", "Saludo", MessageType.MASIVO, 1,
            "Hola {{nombre}}, {{mensaje}}", List.of(TemplateVariable.builder().name("nombre").build(),
                    TemplateVariable.builder().name("mensaje").build()));

    private final PlantillaContenidoService service = new PlantillaContenidoService(new PlantillaQueryPort() {
        @Override
        public Optional<CompiledTemplate> findActive(String code) {
            return "SALUDO".equalsIgnoreCase(code) ? Optional.of(SALUDO) : Optional.empty();
        }

        @Override
        public void refresh() {
        }
    }, new TemplateRenderer());

    @Test
    void usaElTipoDeLaPlantillaYRechazaUnTipoDistinto() {
        ResolvedContent c = service.resolve("saludo", Map.of("nombre", "Ana", "mensaje", "bienvenida"), null);
        assertEquals(MessageType.MASIVO, c.type());
        assertEquals("Hola Ana, bienvenida", c.text());
        SmsContenidoException e = assertThrows(SmsContenidoException.class,
                () -> service.resolve("SALUDO", Map.of("nombre", "Ana", "mensaje", "x"), MessageType.OTP));
        assertEquals(SmsContenidoException.VALIDACION, e.getCodigo());
    }

    @Test
    void plantillaInexistente() {
        SmsContenidoException e = assertThrows(SmsContenidoException.class, () -> service.resolve("NO", Map.of(), null));
        assertEquals(SmsContenidoException.PLANTILLA_NO_ENCONTRADA, e.getCodigo());
    }

    @Test
    void masivoCombinaVariablesYValidaTodosAntesDeEnviar() {
        ResolvedContent c = service.resolveBatch("SALUDO", Map.of("mensaje", "su cita es mañana"),
                List.of(Map.of("nombre", "Ana"), Map.of("nombre", "Luis", "mensaje", "llamenos")), null);
        assertEquals(List.of("Hola Ana, su cita es mañana", "Hola Luis, llamenos"), c.texts());

        SmsContenidoException e = assertThrows(SmsContenidoException.class, () -> service.resolveBatch("SALUDO",
                Map.of("mensaje", "x"), Arrays.asList(Map.of("nombre", "Ana"), null, Map.of("nombre", "B", "otra", "y")), null));
        assertEquals(SmsContenidoException.VARIABLES_INVALIDAS, e.getCodigo());
        assertTrue(e.getMessage().startsWith("2 de 3"));
        assertEquals("destinations[1].nombre", e.getErrores().get(0).getVariable());
        assertEquals("destinations[2].otra", e.getErrores().get(1).getVariable());
    }
}
