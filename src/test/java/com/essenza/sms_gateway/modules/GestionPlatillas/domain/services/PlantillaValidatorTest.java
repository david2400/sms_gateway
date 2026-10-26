package com.essenza.sms_gateway.modules.GestionPlatillas.domain.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception.PlantillaValidacionException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.Plantilla;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.PlantillaVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TipoMensajeRef;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.VariableDataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantillaValidatorTest {

    private final PlantillaValidator validator = new PlantillaValidator(codigo -> switch (codigo) {
        case "NORMAL" -> Optional.of(new TipoMensajeRef(5L, "NORMAL", false));
        case "PROPIO" -> Optional.of(new TipoMensajeRef(9L, "PROPIO", false));
        default -> Optional.empty();
    });

    @Test
    void sinVariablesLasCreaDesdeElContenido() {
        Plantilla p = plantilla("Hola {{nombre}}, su cita es {{fecha}}", "normal", null);
        validator.validar(p);
        assertEquals("RECORDATORIO", p.getCodigo());
        assertEquals(List.of("nombre", "fecha"), p.getVariables().stream().map(PlantillaVariable::getNombre).toList());
        assertEquals(VariableDataType.TEXTO, p.getVariables().get(0).getTipoDato());
        assertEquals(5L, p.getTipoMensajeId());
    }

    @Test
    void reportaTodasLasInconsistencias() {
        List<PlantillaVariable> vars = new ArrayList<>(List.of(
                var("nombre", VariableDataType.TEXTO, null, null),
                var("sobrante", VariableDataType.TEXTO, null, null),
                var("monto", VariableDataType.MONEDA, null, "abc"),
                var("codigo", VariableDataType.TEXTO, "[0-9", null)));
        Plantilla p = plantilla("Hola {{nombre}} {{monto}} {{codigo}} {{faltante}} {{", "PROPIO", vars);

        PlantillaValidacionException e = assertThrows(PlantillaValidacionException.class, () -> validator.validar(p));
        List<String> d = e.getDetalles();
        assertTrue(d.stream().anyMatch(x -> x.contains("sin cerrar")), d.toString());
        assertTrue(d.stream().anyMatch(x -> x.contains("{{faltante}}")), d.toString());
        assertTrue(d.stream().anyMatch(x -> x.contains("'sobrante' está declarada")), d.toString());
        assertTrue(d.stream().anyMatch(x -> x.contains("valor por defecto de 'monto'")), d.toString());
        assertTrue(d.stream().anyMatch(x -> x.contains("Patrón inválido en 'codigo'")), d.toString());
        assertTrue(d.stream().anyMatch(x -> x.contains("no está soportado por el motor")), d.toString());
    }

    private static Plantilla plantilla(String contenido, String tipo, List<PlantillaVariable> vars) {
        Plantilla p = new Plantilla();
        p.setCodigo("recordatorio");
        p.setNombre("R");
        p.setContenido(contenido);
        p.setTipoMensaje(tipo);
        p.setVariables(vars);
        return p;
    }

    private static PlantillaVariable var(String nombre, VariableDataType tipo, String patron, String def) {
        return new PlantillaVariable(null, nombre, tipo, true, null, patron, def, null, null);
    }
}
