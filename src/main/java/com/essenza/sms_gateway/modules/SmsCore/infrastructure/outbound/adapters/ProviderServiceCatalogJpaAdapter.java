package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderServiceCatalogPort;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.config.SmsCoreProperties;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsProveedorServicioEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.SmsProveedorServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo de servicios por proveedor desde {@code SmsProveedorServicio}, cacheado completo en memoria
 * con el mismo TTL de las rutas ({@code sms.core.routing-cache-ttl-seconds}). Se recarga con
 * {@code POST /v1/sms/admin/routing-cache/refresh} o tras cualquier cambio en SmsAdmin.
 * Tipos en BD que no existen en {@link MessageType} se ignoran (no se pueden pedir por API).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderServiceCatalogJpaAdapter implements ProviderServiceCatalogPort {

    private final SmsProveedorServicioRepository repository;
    private final SmsCoreProperties properties;

    private volatile Snapshot snapshot;

    @Override
    public boolean supports(String providerCode, MessageType type) {
        return providerCode != null && type != null && servicesOf(providerCode).contains(type);
    }

    @Override
    public Set<MessageType> servicesOf(String providerCode) {
        if (providerCode == null) {
            return Set.of();
        }
        return current().byProvider().getOrDefault(providerCode.trim().toUpperCase(Locale.ROOT), Set.of());
    }

    @Override
    public void refresh() {
        snapshot = null;
        log.info("[SMS-CORE] Caché de servicios por proveedor descartada");
    }

    private Snapshot current() {
        Snapshot s = snapshot;
        long ttlMs = properties.getRoutingCacheTtlSeconds() * 1000L;
        if (s != null && ttlMs > 0 && s.loadedAt().plusMillis(ttlMs).isAfter(Instant.now())) {
            return s;
        }
        synchronized (this) {
            s = snapshot;
            if (s == null || ttlMs <= 0 || !s.loadedAt().plusMillis(ttlMs).isAfter(Instant.now())) {
                s = load();
                snapshot = s;
            }
            return s;
        }
    }

    private Snapshot load() {
        Map<String, Set<MessageType>> map = new HashMap<>();
        for (SmsProveedorServicioEntity ps : repository.findAllActive()) {
            if (!Boolean.FALSE.equals(ps.getTipoMensaje().getEliminado())) {
                continue;
            }
            Optional<MessageType> type = MessageType.fromCodigo(ps.getTipoMensaje().getCodigo());
            type.ifPresent(t -> map.computeIfAbsent(ps.getProveedor().getCodigo().toUpperCase(Locale.ROOT),
                    k -> EnumSet.noneOf(MessageType.class)).add(t));
        }
        Map<String, Set<MessageType>> frozen = new HashMap<>();
        map.forEach((k, v) -> frozen.put(k, Collections.unmodifiableSet(v)));
        log.debug("[SMS-CORE] Servicios por proveedor cargados: {}", frozen);
        return new Snapshot(Collections.unmodifiableMap(frozen), Instant.now());
    }

    record Snapshot(Map<String, Set<MessageType>> byProvider, Instant loadedAt) {
    }
}
