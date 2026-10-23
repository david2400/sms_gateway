package com.essenza.sms_gateway.modules.GestionPlatillas.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.CompiledTemplate;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.RenderedSms;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.SmsEncoding;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TemplateVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.VariableDataType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.VariableError;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    private static CompiledTemplate cita() {
        return TemplateParser.compile(1L, "RECORDATORIO_CITA", "Cita", MessageType.NORMAL, 3,
                "Hola {{nombre}}, su cita es el {{ fecha }} a las {{hora}} en {{sede}}. Valor {{valor}}",
                List.of(
                        TemplateVariable.builder().name("nombre").maxLength(20).build(),
                        TemplateVariable.builder().name("fecha").dataType(VariableDataType.FECHA).build(),
                        TemplateVariable.builder().name("hora").dataType(VariableDataType.HORA).build(),
                        TemplateVariable.builder().name("sede").required(false).defaultValue("Sede Norte").build(),
                        TemplateVariable.builder().name("valor").dataType(VariableDataType.MONEDA).build()));
    }

    @Test
    void rendersConFormatoPorTipoYValorPorDefecto() {
        RenderedSms r = renderer.render(cita(), Map.of("nombre", "Ana", "fecha", "2026-11-05", "hora", "14:30",
                "valor", "125000"));

        assertEquals("Hola Ana, su cita es el 05/11/2026 a las 2:30 PM en Sede Norte. Valor $125.000", r.getText());
        assertEquals(SmsEncoding.GSM7, r.getEncoding());
        assertEquals(1, r.getParts());
        assertEquals(3, r.getTemplateVersion());
    }

    @Test
    void reportaTodosLosErroresJuntos() {
        SmsContenidoException e = assertThrows(SmsContenidoException.class, () -> renderer.render(cita(),
                Map.of("nombre", "Un nombre demasiado largo para la variable", "fecha", "05-11-2026", "valor", "abc",
                        "extra", "x")));

        assertEquals(SmsContenidoException.VARIABLES_INVALIDAS, e.getCodigo());
        Map<String, String> byVar = e.getErrores().stream()
                .collect(Collectors.toMap(VariableError::getVariable, VariableError::getCodigo));
        assertEquals(VariableError.DESCONOCIDA, byVar.get("extra"));
        assertEquals(VariableError.LONGITUD_EXCEDIDA, byVar.get("nombre"));
        assertEquals(VariableError.TIPO_INVALIDO, byVar.get("fecha"));
        assertEquals(VariableError.FALTANTE, byVar.get("hora"));
        assertEquals(VariableError.TIPO_INVALIDO, byVar.get("valor"));
        assertEquals(5, e.getErrores().size());
    }

    @Test
    void patronSeValidaSobreElValorCompleto() {
        CompiledTemplate t = TemplateParser.compile(1L, "OTP_X", "OTP", MessageType.OTP, 1, "Su código es {{codigo}}",
                List.of(TemplateVariable.builder().name("codigo").pattern("\\d{6}").build()));

        assertEquals("Su código es 123456", renderer.render(t, Map.of("codigo", "123456")).getText());
        SmsContenidoException e = assertThrows(SmsContenidoException.class, () -> renderer.render(t, Map.of("codigo", "12345a")));
        assertEquals(VariableError.FORMATO_INVALIDO, e.getErrores().get(0).getCodigo());
    }

    @Test
    void tildesObliganUcs2YCambianLasPartes() {
        CompiledTemplate t = TemplateParser.compile(1L, "T", "T", null, 1, "{{texto}}",
                List.of(TemplateVariable.builder().name("texto").build()));

        RenderedSms r = renderer.render(t, Map.of("texto", "Información ".repeat(7)));

        assertEquals(SmsEncoding.UCS2, r.getEncoding());
        assertEquals(83, r.getLength()); // TEXTO recorta el espacio final
        assertEquals(2, r.getParts());
    }

    @Test
    void parserDetectaErroresDeSintaxisYVariables() {
        TemplateParser.Parsed p = TemplateParser.parse("Hola {{nombre}} {{1malo}} {{ sin cierre");
        assertFalse(p.isValid());
        assertEquals(Set.of("nombre"), p.variables());
        assertEquals(2, p.errors().size());

        assertEquals("Llaves {{literal}}", TemplateParser.parse("Llaves \\{{literal}}").segments().get(0).literal());
        assertThrows(IllegalArgumentException.class, () -> TemplateParser.compile(1L, "X", "X", null, 1, "{{a}} {{b}}",
                List.of(TemplateVariable.builder().name("a").build())));
    }

    @Test
    void gsmExtendidoCuentaDoble() {
        assertEquals(SmsEncoding.GSM7, SmsSegmentCalculator.encodingOf("Precio 10€ [promo]"));
        assertEquals(21, SmsSegmentCalculator.lengthOf("Precio 10€ [promo]", SmsEncoding.GSM7));
        assertTrue(SmsSegmentCalculator.partsOf(161, SmsEncoding.GSM7) == 2);
    }
}
