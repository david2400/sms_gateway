package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteLookup;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.config.SmsCoreProperties;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.mappers.ProviderAccountFactory;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.CanalCuentaEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.CanalProveedorEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.RutaEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.RutaPasoEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.RutaPasoRepository;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.RutaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lee rutas, pasos y cuentas de BD y las cachea en memoria (TTL configurable).
 * <p>
 * Reglas aplicadas al construir la ruta:
 * <ul>
 *   <li>Se omiten pasos, cuentas o proveedores inactivos.</li>
 *   <li>En una ruta de proveedor solo se aceptan cuentas de ese proveedor.</li>
 *   <li>La contraseña se resuelve desde {@code secret_ref} (variable de entorno).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingPolicyJpaAdapter implements RoutingPolicyPort {

    private static final String CENTRAL = "*CENTRAL*";
    private static final String POR_REFERENCIA = "*RUTA*|";

    private final RutaRepository rutaRepository;
    private final RutaPasoRepository pasoRepository;
    private final ProviderAccountFactory accountFactory;
    private final SmsCoreProperties properties;

    private final Map<String, CachedPlan> cache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public RoutePlan resolve(String providerCode, MessageType type) {
        String key = (providerCode == null ? CENTRAL : providerCode.toUpperCase()) + "|" + type;
        long ttlMs = properties.getRoutingCacheTtlSeconds() * 1000L;

        CachedPlan cached = cache.get(key);
        if (cached != null && ttlMs > 0 && cached.loadedAt().plusMillis(ttlMs).isAfter(Instant.now())) {
            return cached.plan();
        }

        RoutePlan plan = load(providerCode, type);
        // No se cachea un plan vacío: si la ruta se corrige en BD, el siguiente envío la toma sin esperar el TTL
        if (ttlMs > 0 && !plan.isEmpty()) {
            cache.put(key, new CachedPlan(plan, Instant.now()));
        }
        return plan;
    }

    /**
     * Ruta pedida en el request ({@code ruta}): por id (si es numérico) o por {@code codigo}. Misma caché y mismas reglas
     * de pasos que {@link #resolve}.
     */
    @Override
    @Transactional(readOnly = true)
    public RouteLookup findRoute(String reference) {
        String ref = reference == null ? "" : reference.trim();
        if (ref.isEmpty()) {
            return RouteLookup.notFound(reference);
        }
        String key = POR_REFERENCIA + ref.toUpperCase();
        long ttlMs = properties.getRoutingCacheTtlSeconds() * 1000L;
        CachedPlan cached = cache.get(key);
        if (cached != null && ttlMs > 0 && cached.loadedAt().plusMillis(ttlMs).isAfter(Instant.now())) {
            return RouteLookup.found(ref, cached.plan());
        }

        Long id = ref.chars().allMatch(Character::isDigit) && ref.length() <= 18 ? Long.valueOf(ref) : null;
        Optional<RutaEntity> ruta = id != null ? rutaRepository.findById(id) : Optional.empty();
        if (ruta.isEmpty()) {
            ruta = rutaRepository.findFirstByCodigoIgnoreCase(ref);
        }
        if (ruta.isEmpty()) {
            // @SoftDelete oculta las inactivas: se consulta el estado para distinguir "inactiva" de "no existe"
            List<Boolean> estado = rutaRepository.findEstado(id != null ? id : -1L, ref);
            return !estado.isEmpty() && Boolean.TRUE.equals(estado.get(0)) ? RouteLookup.inactive(ref) : RouteLookup.notFound(ref);
        }

        RutaEntity r = ruta.get();
        String providerCode = r.getCanalProveedor() == null ? null : r.getCanalProveedor().getCodigo();
        MessageType type;
        try {
            type = MessageType.valueOf(r.getTipoMensaje().getCodigo());
        } catch (IllegalArgumentException e) {
            log.warn("[SMS-CORE] La ruta {} tiene el tipo {} que no existe en el motor", ref, r.getTipoMensaje().getCodigo());
            return RouteLookup.notFound(ref);
        }
        RoutePlan plan = build(r, providerCode, type);
        if (ttlMs > 0 && !plan.isEmpty()) {
            cache.put(key, new CachedPlan(plan, Instant.now()));
        }
        return RouteLookup.found(ref, plan);
    }

    @Override
    public void refresh() {
        cache.clear();
        log.info("[SMS-CORE] Caché de rutas SMS descartada");
    }

    private RoutePlan load(String providerCode, MessageType type) {
        Optional<RutaEntity> ruta = providerCode == null
                ? rutaRepository.findFirstByCanalProveedorIsNullAndTipoMensaje_CodigoAndEliminadoFalse(type.name())
                : rutaRepository.findFirstByCanalProveedor_CodigoAndTipoMensaje_CodigoAndEliminadoFalse(providerCode.toUpperCase(), type.name());

        if (ruta.isEmpty()) {
            return RoutePlan.empty(providerCode, type);
        }
        return build(ruta.get(), providerCode, type);
    }

    /** Pasos activos de la ruta en el orden de {@code orden}, omitiendo cuentas/proveedores inactivos o de otro proveedor. */
    private RoutePlan build(RutaEntity ruta, String providerCode, MessageType type) {
        RoutePlan.RoutePlanBuilder builder = RoutePlan.builder()
                .routeId(ruta.getIdRuta())
                .providerCode(providerCode)
                .messageType(type);

        for (RutaPasoEntity paso : pasoRepository.findByRuta_IdRutaAndEliminadoFalseOrderByOrdenAsc(ruta.getIdRuta())) {
            CanalCuentaEntity cuenta = paso.getCanalCuenta();
            CanalProveedorEntity proveedor = cuenta.getCanalProveedor();

            if (cuenta.isEliminado() || proveedor.isEliminado()) {
                continue;
            }
            if (providerCode != null && !providerCode.equalsIgnoreCase(proveedor.getCodigo())) {
                log.warn("[SMS-CORE] Ruta {} de {} tiene la cuenta {} de otro proveedor ({}); se ignora",
                        ruta.getIdRuta(), providerCode, cuenta.getCodigo(), proveedor.getCodigo());
                continue;
            }

            builder.step(RouteStep.builder()
                    .order(paso.getOrden())
                    .maxAttempts(paso.getMaxIntentos() != null ? paso.getMaxIntentos() : 3)
                    .waitMs(paso.getEsperaMs() != null ? paso.getEsperaMs() : 500)
                    .account(accountFactory.toAccount(cuenta))
                    .build());
        }
        return builder.build();
    }

    private record CachedPlan(RoutePlan plan, Instant loadedAt) {
    }
}
