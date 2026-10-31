package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.PlanResolution;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteLookup;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderAccountQueryPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderServiceCatalogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decide QUÉ cuentas se usan para un envío, antes de gastar intentos. Lo comparten el envío
 * individual ({@link SmsDispatchService}) y el masivo ({@link SmsBatchDispatchService}).
 * <ol>
 *   <li>Canal PROVEEDOR: el proveedor debe tener el servicio (tipo) habilitado → si no, {@code SERVICIO_NO_SOPORTADO}.</li>
 *   <li>Canal CENTRAL con proveedor preferido: igual validación; sus cuentas van primero y la ruta central queda de respaldo.</li>
 *   <li>Se quitan de la ruta las cuentas cuyo proveedor no tiene el servicio: el fallback solo pasa por
 *       proveedores que soportan el MISMO tipo de envío.</li>
 *   <li>Cuenta pedida: debe existir, estar activa, ser del proveedor (canal PROVEEDOR / preferido) y su proveedor
 *       debe tener el servicio. Va primero y el resto de la ruta queda de respaldo.</li>
 *   <li>Ruta vacía: {@code SERVICIO_NO_SOPORTADO} si se vació por el filtro de servicios, si no {@code RUTA_NO_CONFIGURADA}.</li>
 * </ol>
 * Con una ruta pedida en el request ({@link #planRoute}) se ejecutan los pasos de ESA ruta en su orden; solo se
 * omiten cuentas/proveedores inactivos o sin el servicio del tipo de la ruta.
 */
@Slf4j
@Service
public class DispatchPlanner {

    /** Intentos y espera para una cuenta que no está en la ruta (mismos defaults de {@code SmsRutaPaso}). */
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_WAIT_MS = 500;

    private final RoutingPolicyPort routingPolicy;
    private final ProviderAccountQueryPort accountQuery;
    private final ProviderServiceCatalogPort serviceCatalog;

    public DispatchPlanner(RoutingPolicyPort routingPolicy,
                           ProviderAccountQueryPort accountQuery,
                           ProviderServiceCatalogPort serviceCatalog) {
        this.routingPolicy = routingPolicy;
        this.accountQuery = accountQuery;
        this.serviceCatalog = serviceCatalog;
    }

    /**
     * @param type              tipo de mensaje (no nulo)
     * @param providerCode      proveedor del canal PROVEEDOR, o {@code null} para el canal CENTRAL
     * @param preferredProvider proveedor preferido (solo canal CENTRAL), opcional
     * @param requestedAccount  cuenta pedida en el request, opcional
     */
    public PlanResolution plan(MessageType type, String providerCode, String preferredProvider, String requestedAccount) {
        String provider = upper(providerCode);
        if (provider != null && !serviceCatalog.supports(provider, type)) {
            return unsupported(provider, type, null);
        }

        String preferred = provider == null ? upper(preferredProvider) : null;
        if (preferred != null && !serviceCatalog.supports(preferred, type)) {
            return unsupported(preferred, type, null);
        }

        RoutePlan plan = routingPolicy.resolve(provider, type);
        if (preferred != null) {
            plan = withPreferredProvider(plan, routingPolicy.resolve(preferred, type), preferred);
        }

        int before = size(plan);
        plan = onlySupported(plan, type);
        int removed = before - size(plan);

        String account = blankToNull(requestedAccount);
        if (account != null) {
            String owner = provider != null ? provider : preferred;
            Optional<ProviderAccount> found = accountQuery.findActive(owner, account);
            if (found.isEmpty()) {
                log.warn("[SMS-CORE] Cuenta solicitada {} no válida para {}", account, owner == null ? "CENTRAL" : owner);
                return PlanResolution.error(DispatchResult.ERROR_CUENTA_NO_VALIDA,
                        "La cuenta " + account + " no existe, está inactiva"
                                + (owner != null ? " o no pertenece al proveedor " + owner : ""), owner, account);
            }
            ProviderAccount acc = found.get();
            if (!serviceCatalog.supports(acc.getProviderCode(), type)) {
                return unsupported(acc.getProviderCode(), type, acc.getCode());
            }
            plan = withRequestedAccount(plan, acc);
        }

        if (plan.isEmpty()) {
            String name = provider == null ? "CENTRAL" : provider;
            if (removed > 0) {
                log.error("[SMS-CORE] La ruta {}/{} solo tiene cuentas de proveedores sin el servicio {}", name, type, type);
                return PlanResolution.error(DispatchResult.ERROR_SERVICIO_NO_SOPORTADO,
                        "Ninguna cuenta de la ruta " + name + "/" + type + " pertenece a un proveedor con el servicio " + type,
                        provider, null);
            }
            log.error("[SMS-CORE] No hay ruta activa para proveedor={} tipo={}", name, type);
            return PlanResolution.error(DispatchResult.ERROR_RUTA_NO_CONFIGURADA,
                    "No hay ruta activa para " + name + "/" + type, provider, null);
        }
        return PlanResolution.ok(plan);
    }

    /**
     * Plan de una ruta pedida explícitamente ({@code ruta} del request): sus pasos activos en el orden de {@code orden}.
     *
     * @param reference         código o id de la ruta
     * @param requestedType     tipo de mensaje del request, opcional; si viene debe ser el de la ruta
     * @param providerCode      proveedor del canal PROVEEDOR ({@code null} en CENTRAL); la ruta debe ser de ese proveedor
     * @param preferredProvider proveedor preferido del request: no se combina con {@code ruta}
     * @param requestedAccount  cuenta pedida en el request: no se combina con {@code ruta}
     */
    public PlanResolution planRoute(String reference, MessageType requestedType, String providerCode,
                                    String preferredProvider, String requestedAccount) {
        String ref = blankToNull(reference);
        if (blankToNull(preferredProvider) != null || blankToNull(requestedAccount) != null) {
            return PlanResolution.error(DispatchResult.ERROR_RUTA_NO_COMBINABLE,
                    "'ruta' no se puede combinar con 'proveedor' ni con 'cuenta': la ruta ya define cuentas y orden",
                    upper(preferredProvider), blankToNull(requestedAccount));
        }
        RouteLookup lookup = routingPolicy.findRoute(ref);
        if (lookup.getStatus() == RouteLookup.Status.NOT_FOUND) {
            log.warn("[SMS-CORE] Ruta solicitada {} no existe", ref);
            return PlanResolution.error(DispatchResult.ERROR_RUTA_NO_ENCONTRADA,
                    "No existe una ruta de SMS con código o id " + ref, null, null);
        }
        if (lookup.getStatus() == RouteLookup.Status.INACTIVE) {
            log.warn("[SMS-CORE] Ruta solicitada {} está inactiva", ref);
            return PlanResolution.error(DispatchResult.ERROR_RUTA_INACTIVA,
                    "La ruta " + ref + " está inactiva", null, null);
        }

        RoutePlan plan = lookup.getPlan();
        MessageType type = plan.getMessageType();
        String routeProvider = plan.getProviderCode();
        String routeName = (routeProvider == null ? "CENTRAL" : routeProvider) + "/" + type + " (ruta " + ref + ")";
        String provider = upper(providerCode);
        if (provider != null && !provider.equalsIgnoreCase(routeProvider == null ? "" : routeProvider)) {
            return PlanResolution.error(DispatchResult.ERROR_RUTA_NO_COMBINABLE,
                    "La ruta " + ref + " es " + routeName + "; en el endpoint de " + provider
                            + " solo se pueden usar rutas de ese proveedor", provider, null);
        }
        if (requestedType != null && requestedType != type) {
            return PlanResolution.error(DispatchResult.ERROR_RUTA_TIPO_NO_COINCIDE,
                    "La ruta " + ref + " es de tipo " + type + " y el request pide " + requestedType, routeProvider, null);
        }

        int before = size(plan);
        plan = onlySupported(plan, type);
        if (plan.isEmpty()) {
            if (before > 0) {
                log.error("[SMS-CORE] La ruta {} solo tiene cuentas de proveedores sin el servicio {}", routeName, type);
                return PlanResolution.error(DispatchResult.ERROR_SERVICIO_NO_SOPORTADO,
                        "Ninguna cuenta de la ruta " + routeName + " pertenece a un proveedor con el servicio " + type,
                        routeProvider, null);
            }
            log.error("[SMS-CORE] La ruta {} no tiene pasos activos", routeName);
            return PlanResolution.error(DispatchResult.ERROR_RUTA_NO_CONFIGURADA,
                    "La ruta " + routeName + " no tiene pasos activos", routeProvider, null);
        }
        return PlanResolution.ok(plan);
    }

    /** Tipo de mensaje de una ruta activa (para completar el request que no trae {@code tipo_mensaje}). */
    public Optional<MessageType> routeType(String reference) {
        String ref = blankToNull(reference);
        if (ref == null) {
            return Optional.empty();
        }
        RouteLookup lookup = routingPolicy.findRoute(ref);
        return lookup.isFound() ? Optional.ofNullable(lookup.getPlan().getMessageType()) : Optional.empty();
    }

    /** Quita los pasos cuyo proveedor no tiene el servicio y los renumera. */
    RoutePlan onlySupported(RoutePlan plan, MessageType type) {
        List<RouteStep> kept = new ArrayList<>();
        for (RouteStep step : steps(plan)) {
            if (serviceCatalog.supports(step.getAccount().getProviderCode(), type)) {
                kept.add(step);
            } else {
                log.warn("[SMS-CORE] Cuenta {} omitida del fallback: {} no tiene el servicio {}",
                        step.getAccount().getCode(), step.getAccount().getProviderCode(), type);
            }
        }
        return kept.size() == steps(plan).size() ? plan : rebuild(plan, kept);
    }

    /**
     * Pone primero las cuentas del proveedor preferido: las de su ruta propia (si existe) o, si no tiene,
     * las suyas que estén en la ruta central. Luego el resto de la ruta central, sin repetir cuentas.
     */
    static RoutePlan withPreferredProvider(RoutePlan central, RoutePlan providerRoute, String preferred) {
        List<RouteStep> first = !steps(providerRoute).isEmpty()
                ? steps(providerRoute)
                : steps(central).stream()
                    .filter(s -> preferred.equalsIgnoreCase(s.getAccount().getProviderCode()))
                    .toList();
        Set<String> used = first.stream().map(s -> key(s.getAccount())).collect(Collectors.toCollection(HashSet::new));
        List<RouteStep> all = new ArrayList<>(first);
        for (RouteStep step : steps(central)) {
            if (used.add(key(step.getAccount()))) {
                all.add(step);
            }
        }
        return rebuild(central, all);
    }

    /**
     * Pone la cuenta pedida como primer paso y deja el resto de la ruta como respaldo, sin repetirla.
     * Si la cuenta ya estaba en la ruta conserva sus intentos y espera; si no, usa los valores por defecto.
     * Los pasos se renumeran en el orden en que se ejecutarán (es el {@code paso_orden} del log).
     */
    static RoutePlan withRequestedAccount(RoutePlan plan, ProviderAccount requested) {
        RouteStep inRoute = steps(plan).stream()
                .filter(s -> key(s.getAccount()).equals(key(requested)))
                .findFirst().orElse(null);
        List<RouteStep> all = new ArrayList<>();
        all.add(RouteStep.builder()
                .account(requested)
                .maxAttempts(inRoute != null ? inRoute.getMaxAttempts() : DEFAULT_MAX_ATTEMPTS)
                .waitMs(inRoute != null ? inRoute.getWaitMs() : DEFAULT_WAIT_MS)
                .build());
        for (RouteStep step : steps(plan)) {
            if (!key(step.getAccount()).equals(key(requested))) {
                all.add(step);
            }
        }
        return rebuild(plan, all);
    }

    private static RoutePlan rebuild(RoutePlan base, List<RouteStep> steps) {
        RoutePlan.RoutePlanBuilder builder = RoutePlan.builder()
                .routeId(base.getRouteId())
                .providerCode(base.getProviderCode())
                .messageType(base.getMessageType());
        int order = 1;
        for (RouteStep s : steps) {
            builder.step(RouteStep.builder()
                    .order(order++)
                    .account(s.getAccount())
                    .maxAttempts(s.getMaxAttempts())
                    .waitMs(s.getWaitMs())
                    .build());
        }
        return builder.build();
    }

    private PlanResolution unsupported(String provider, MessageType type, String accountCode) {
        Set<MessageType> available = serviceCatalog.servicesOf(provider);
        String detail = "El proveedor " + provider + " no cuenta con el servicio " + type
                + (accountCode != null ? " (cuenta " + accountCode + ")" : "")
                + (available.isEmpty() ? "; no tiene servicios habilitados"
                : "; servicios habilitados: " + available.stream().map(Enum::name).sorted().collect(Collectors.joining(", ")));
        log.warn("[SMS-CORE] {}", detail);
        return PlanResolution.error(DispatchResult.ERROR_SERVICIO_NO_SOPORTADO, detail, provider, accountCode);
    }

    private static List<RouteStep> steps(RoutePlan plan) {
        return plan == null || plan.getSteps() == null ? List.of() : plan.getSteps();
    }

    private static int size(RoutePlan plan) {
        return steps(plan).size();
    }

    private static String key(ProviderAccount account) {
        return account.getCode() == null ? "" : account.getCode().toUpperCase(Locale.ROOT);
    }

    private static String upper(String value) {
        String v = blankToNull(value);
        return v == null ? null : v.toUpperCase(Locale.ROOT);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
