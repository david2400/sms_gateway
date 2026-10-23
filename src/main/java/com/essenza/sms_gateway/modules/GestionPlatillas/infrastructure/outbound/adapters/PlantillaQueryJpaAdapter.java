package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.CompiledTemplate;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TemplateVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.VariableDataType;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaQueryPort;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.services.TemplateParser;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.config.GestionPlantillasProperties;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsEntity;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsVariableEntity;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository.PlantillaSmsRepository;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lee plantillas activas de BD, las compila UNA vez y las cachea por código ({@code sms.plantillas.cache-ttl-seconds}).
 * Las ausencias también se cachean (máx. 30 s) para no consultar la BD en cada request con un código inexistente.
 * La administración invalida la caché después de cada commit ({@link PlantillaCacheAdapter}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlantillaQueryJpaAdapter implements PlantillaQueryPort {

    private static final long MISSING_TTL_MS = 30_000;

    private final PlantillaSmsRepository repository;
    private final GestionPlantillasProperties properties;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public Optional<CompiledTemplate> findActive(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String key = code.trim().toUpperCase(Locale.ROOT);
        long ttlMs = properties.getCacheTtlSeconds() * 1000L;
        Cached cached = cache.get(key);
        if (cached != null && cached.isFresh(ttlMs)) {
            return Optional.ofNullable(cached.template());
        }
        CompiledTemplate template = repository.findByCodigoAndActivoTrue(key).map(this::compile).orElse(null);
        if (ttlMs > 0) {
            cache.put(key, new Cached(template, Instant.now()));
        }
        return Optional.ofNullable(template);
    }

    @Override
    public void refresh() {
        cache.clear();
        log.info("[PLANTILLAS] Caché de plantillas SMS descartada");
    }

    private CompiledTemplate compile(PlantillaSmsEntity e) {
        try {
            List<TemplateVariable> vars = e.getVariables().stream().map(PlantillaQueryJpaAdapter::toVariable).toList();
            MessageType type = e.getTipoMensaje() == null ? null
                    : MessageType.fromCodigo(e.getTipoMensaje().getCodigo()).orElse(null);
            return TemplateParser.compile(e.getIdSmsPlantilla(), e.getCodigo(), e.getNombre(), type,
                    e.getVersion() != null ? e.getVersion() : 1, e.getContenido(), vars);
        } catch (IllegalArgumentException ex) {
            log.error("[PLANTILLAS] Plantilla {} mal configurada: {}", e.getCodigo(), ex.getMessage());
            throw new SmsContenidoException(SmsContenidoException.PLANTILLA_MAL_CONFIGURADA, e.getCodigo(),
                    "La plantilla " + e.getCodigo() + " tiene errores de configuración: " + ex.getMessage(), List.of());
        }
    }

    private static TemplateVariable toVariable(PlantillaSmsVariableEntity v) {
        VariableDataType type;
        try {
            type = VariableDataType.valueOf(v.getTipoDato() == null ? "TEXTO" : v.getTipoDato().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de dato inválido '" + v.getTipoDato() + "' en la variable " + v.getNombre());
        }
        return TemplateVariable.builder()
                .name(v.getNombre())
                .dataType(type)
                .required(!Boolean.FALSE.equals(v.getObligatoria()))
                .maxLength(v.getLongitudMax())
                .pattern(v.getPatron())
                .defaultValue(v.getValorDefecto())
                .description(v.getDescripcion())
                .build();
    }

    private record Cached(CompiledTemplate template, Instant loadedAt) {
        boolean isFresh(long ttlMs) {
            long ttl = template == null ? Math.min(ttlMs, MISSING_TTL_MS) : ttlMs;
            return ttl > 0 && loadedAt.plusMillis(ttl).isAfter(Instant.now());
        }
    }
}
