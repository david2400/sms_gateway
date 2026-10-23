package com.essenza.sms_gateway.modules.GestionPlatillas.domain.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception.PlantillaValidacionException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.Plantilla;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.PlantillaVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TipoMensajeRef;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.TipoMensajeConsultaPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TemplateVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.VariableDataType;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.services.TemplateParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reglas de una plantilla antes de guardarla (usa el mismo parser que el motor de SmsCore, así lo que se guarda
 * siempre se puede enviar):
 * <ul>
 *   <li>Sintaxis {@code {{variable}}} válida.</li>
 *   <li>Si no se declaran variables se crean desde el contenido (TEXTO, obligatorias).</li>
 *   <li>Toda variable usada está declarada y toda variable declarada se usa (sin nombres repetidos).</li>
 *   <li>Patrones compilables; valor por defecto válido para su tipo, longitud y patrón.</li>
 *   <li>Tipo de mensaje existente, activo y soportado por el motor.</li>
 * </ul>
 * Reporta todos los errores juntos en {@code detalles}.
 */
@Service
@RequiredArgsConstructor
public class PlantillaValidator {

    private final TipoMensajeConsultaPort tipos;

    /** Valida y completa defaults (código normalizado, variables, orden, tipo). Modifica la plantilla recibida. */
    public Plantilla validar(Plantilla p) {
        List<String> errores = new ArrayList<>();
        p.setCodigo(CodigosPlantilla.normalizar(p.getCodigo()));

        TemplateParser.Parsed parsed = TemplateParser.parse(p.getContenido());
        parsed.errors().forEach(e -> errores.add(e.getMensaje()));

        if (p.getVariables() == null || p.getVariables().isEmpty()) {
            List<PlantillaVariable> auto = new ArrayList<>();
            for (String name : parsed.variables()) {
                auto.add(new PlantillaVariable(null, name, VariableDataType.TEXTO, true, null, null, null, null, null));
            }
            p.setVariables(auto);
        }

        Set<String> declaradas = new HashSet<>();
        int orden = 0;
        for (PlantillaVariable v : p.getVariables()) {
            v.setOrden(orden++);
            if (v.getTipoDato() == null) {
                v.setTipoDato(VariableDataType.TEXTO);
            }
            if (v.getObligatoria() == null) {
                v.setObligatoria(Boolean.TRUE);
            }
            v.setPatron(CodigosPlantilla.blankToNull(v.getPatron()));
            v.setValorDefecto(CodigosPlantilla.blankToNull(v.getValorDefecto()));
            if (v.getNombre() == null || !TemplateParser.VARIABLE_NAME.matcher(v.getNombre()).matches()) {
                errores.add("Nombre de variable inválido: '" + v.getNombre() + "'");
                continue;
            }
            if (!declaradas.add(v.getNombre().toLowerCase(Locale.ROOT))) {
                errores.add("La variable '" + v.getNombre() + "' está repetida");
                continue;
            }
            validarVariable(v, errores);
        }

        Set<String> declaradasExactas = new HashSet<>();
        p.getVariables().forEach(v -> declaradasExactas.add(v.getNombre()));
        for (String usada : parsed.variables()) {
            if (!declaradasExactas.contains(usada)) {
                errores.add("La variable {{" + usada + "}} se usa en el contenido pero no está declarada");
            }
        }
        for (PlantillaVariable v : p.getVariables()) {
            if (v.getNombre() != null && !parsed.variables().contains(v.getNombre())) {
                errores.add("La variable '" + v.getNombre() + "' está declarada pero no se usa en el contenido");
            }
        }

        resolverTipo(p, errores);

        if (!errores.isEmpty()) {
            throw new PlantillaValidacionException("La plantilla " + p.getCodigo() + " tiene " + errores.size() + " error(es)", errores);
        }
        return p;
    }

    private static void validarVariable(PlantillaVariable v, List<String> errores) {
        Pattern pattern = null;
        if (v.getPatron() != null) {
            try {
                pattern = Pattern.compile(v.getPatron());
            } catch (PatternSyntaxException e) {
                errores.add("Patrón inválido en '" + v.getNombre() + "': " + e.getDescription());
                return;
            }
        }
        String def = v.getValorDefecto();
        if (def == null) {
            return;
        }
        if (v.getLongitudMax() != null && def.length() > v.getLongitudMax()) {
            errores.add("El valor por defecto de '" + v.getNombre() + "' supera longitud_max " + v.getLongitudMax());
        }
        if (pattern != null && !pattern.matcher(def).matches()) {
            errores.add("El valor por defecto de '" + v.getNombre() + "' no cumple el patrón");
        }
        try {
            v.getTipoDato().format(def);
        } catch (IllegalArgumentException e) {
            errores.add("El valor por defecto de '" + v.getNombre() + "' " + e.getMessage());
        }
    }

    private void resolverTipo(Plantilla p, List<String> errores) {
        String codigo = CodigosPlantilla.normalizar(CodigosPlantilla.blankToNull(p.getTipoMensaje()));
        if (codigo == null) {
            p.setTipoMensaje(null);
            p.setTipoMensajeId(null);
            return;
        }
        Optional<TipoMensajeRef> tipo = tipos.findByCodigo(codigo);
        if (tipo.isEmpty() || !tipo.get().activo()) {
            errores.add("El tipo de mensaje " + codigo + " no existe o está inactivo");
            return;
        }
        if (MessageType.fromCodigo(codigo).isEmpty()) {
            errores.add("El tipo de mensaje " + codigo + " no está soportado por el motor de envío");
            return;
        }
        p.setTipoMensaje(tipo.get().codigo());
        p.setTipoMensajeId(tipo.get().id());
    }

    /** Convierte a variables de SmsCore (para previsualizar o probar con el motor). */
    public static List<TemplateVariable> toCore(List<PlantillaVariable> variables) {
        return variables.stream().map(v -> TemplateVariable.builder()
                .name(v.getNombre())
                .dataType(v.getTipoDato())
                .required(!Boolean.FALSE.equals(v.getObligatoria()))
                .maxLength(v.getLongitudMax())
                .pattern(v.getPatron())
                .defaultValue(v.getValorDefecto())
                .description(v.getDescripcion())
                .build()).toList();
    }

}
