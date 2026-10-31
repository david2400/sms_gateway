package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteLookup;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderAccountQueryPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderServiceCatalogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsDispatchLogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas del motor: 3 intentos por cuenta, se detiene al primer éxito,
 * luego cambia de cuenta/proveedor según la ruta, y todo queda en el log.
 */
class SmsDispatchServiceTest {

    private FakeRouting routing;
    private FakeLog log;
    private FakeAccounts accounts;
    private FakeCatalog catalog;
    private ScriptedSender provA;
    private ScriptedSender provB;
    private SmsDispatchService service;

    @BeforeEach
    void setUp() {
        routing = new FakeRouting();
        log = new FakeLog();
        accounts = new FakeAccounts();
        catalog = new FakeCatalog();
        provA = new ScriptedSender("PROV_A");
        provB = new ScriptedSender("PROV_B");
        service = new SmsDispatchService(new DispatchPlanner(routing, accounts, catalog), log, List.of(provA, provB)) {
            @Override
            protected boolean pause(long millis) {
                return true; // sin esperas en pruebas
            }
        };
    }

    @Test
    void segundoIntentoExitosoNoHaceMasEnvios() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"));
        provA.script(500, 200);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("A_OTP", r.getAccountCode());
        assertEquals(2, r.getAttempts());
        assertEquals(2, provA.calls);
        assertEquals(0, provB.calls);
        assertEquals(List.of("A_OTP#1:false", "A_OTP#2:true"), log.attempts);
    }

    @Test
    void tresFallosCambiaDeProveedor() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"));
        provA.script(500, 503, null);
        provB.script(200);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("PROV_B", r.getProviderCode());
        assertEquals("B_OTP", r.getAccountCode());
        assertEquals(4, r.getAttempts());
        assertEquals(3, provA.calls);
        assertEquals(1, provB.calls);
        assertEquals("B_OTP", log.finalAccount);
    }

    @Test
    void todasLasCuentasFallan() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"));
        provA.script(500, 500, 500);
        provB.script(401, 401, 401);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.FALLIDO, r.getStatus());
        assertEquals(DispatchResult.ERROR_TODAS_FALLARON, r.getErrorCode());
        assertEquals(6, r.getAttempts());
        assertEquals(401, r.getHttpStatus());
        assertEquals(6, log.attempts.size());
    }

    @Test
    void rutaNoConfigurada() {
        routing.plan = RoutePlan.empty("PROV_A", MessageType.OTP);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.PROVEEDOR, "PROV_A");

        assertEquals(DispatchStatus.FALLIDO, r.getStatus());
        assertEquals(DispatchResult.ERROR_RUTA_NO_CONFIGURADA, r.getErrorCode());
        assertEquals("PROV_A", routing.lastProvider);
        assertEquals(0, provA.calls);
    }

    @Test
    void numeroInvalidoNoGastaIntentos() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));

        DispatchResult r = service.dispatch(msg().toBuilder().to("12ab").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchResult.ERROR_VALIDACION, r.getErrorCode());
        assertEquals(0, provA.calls);
        assertTrue(log.started);
    }

    @Test
    void proveedorSinModuloSeSalta() {
        routing.plan = plan(step(1, "X_OTP", "PROV_X"), step(2, "A_OTP", "PROV_A"));
        provA.script(200);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("A_OTP", r.getAccountCode());
    }

    @Test
    void idempotencyKeyRepetidaNoReenvia() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));
        UUID previo = UUID.randomUUID();
        log.byKey.put("abc", DispatchResult.builder().envioId(previo).status(DispatchStatus.ENVIADO).build());

        DispatchResult r = service.dispatch(msg().toBuilder().idempotencyKey("abc").build(), DispatchChannel.CENTRAL, null);

        assertEquals(previo, r.getEnvioId());
        assertEquals(0, provA.calls);
    }

    @Test
    void excepcionDelProveedorCuentaComoFallo() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"));
        provA.throwAlways = true;
        provB.script(200);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals(3, provA.calls);
        assertNull(log.attemptStatuses.get(0));
    }

    // ============== cuenta solicitada en el request ==============

    @Test
    void cuentaSolicitadaExitosaNoUsaElRestoDeLaRuta() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"));
        accounts.add(account(2, "B_OTP", "PROV_B"));
        provB.script(200);

        DispatchResult r = service.dispatch(msg().toBuilder().accountCode("B_OTP").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("B_OTP", r.getAccountCode());
        assertEquals(1, r.getAttempts());
        assertEquals(0, provA.calls);
    }

    @Test
    void cuentaSolicitadaVaPrimeroYLuegoSigueLaRutaSinRepetirla() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"));
        accounts.add(account(2, "B_OTP", "PROV_B"));
        provB.script(500, 500, 500);
        provA.script(200);

        DispatchResult r = service.dispatch(msg().toBuilder().accountCode("B_OTP").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("A_OTP", r.getAccountCode());
        assertEquals(4, r.getAttempts());
        assertEquals(3, provB.calls);
        assertEquals(List.of("B_OTP#1:false", "B_OTP#2:false", "B_OTP#3:false", "A_OTP#1:true"), log.attempts);
        assertEquals(List.of(1, 1, 1, 2), log.attemptOrders);
    }

    @Test
    void cuentaSolicitadaFueraDeLaRutaUsaIntentosPorDefecto() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));
        accounts.add(account(9, "B_MASIVO", "PROV_B"));
        provB.script(500, 500, 500);
        provA.script(200);

        DispatchResult r = service.dispatch(msg().toBuilder().accountCode("B_MASIVO").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals(SmsDispatchService.DEFAULT_MAX_ATTEMPTS, provB.calls);
        assertEquals("A_OTP", r.getAccountCode());
    }

    @Test
    void cuentaSolicitadaSinRutaConfiguradaEnviaSoloConEsaCuenta() {
        routing.plan = RoutePlan.empty(null, MessageType.OTP);
        accounts.add(account(2, "B_OTP", "PROV_B"));
        provB.script(500, 200);

        DispatchResult r = service.dispatch(msg().toBuilder().accountCode("B_OTP").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("B_OTP", r.getAccountCode());
        assertEquals(2, r.getAttempts());
    }

    @Test
    void cuentaSolicitadaInexistenteNoEnvia() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));

        DispatchResult r = service.dispatch(msg().toBuilder().accountCode("NO_EXISTE").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.FALLIDO, r.getStatus());
        assertEquals(DispatchResult.ERROR_CUENTA_NO_VALIDA, r.getErrorCode());
        assertEquals(0, provA.calls);
        assertTrue(log.attempts.isEmpty());
    }

    @Test
    void cuentaDeOtroProveedorEnEndpointDeProveedorNoEnvia() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));
        accounts.add(account(2, "B_OTP", "PROV_B"));

        DispatchResult r = service.dispatch(msg().toBuilder().accountCode("B_OTP").build(), DispatchChannel.PROVEEDOR, "PROV_A");

        assertEquals(DispatchResult.ERROR_CUENTA_NO_VALIDA, r.getErrorCode());
        assertEquals("PROV_A", accounts.lastProvider);
        assertEquals(0, provA.calls);
        assertEquals(0, provB.calls);
    }

    @Test
    void sinCuentaNoConsultaCuentas() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));
        provA.script(200);

        service.dispatch(msg().toBuilder().accountCode("  ").build(), DispatchChannel.CENTRAL, null);

        assertEquals(0, accounts.queries);
    }

    // ============== servicios por proveedor ==============

    @Test
    void proveedorSinElServicioRespondeServicioNoSoportado() {
        routing.plan = plan(step(1, "B_OTP", "PROV_B"));
        catalog.deny("PROV_B", MessageType.OTP);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.PROVEEDOR, "PROV_B");

        assertEquals(DispatchResult.ERROR_SERVICIO_NO_SOPORTADO, r.getErrorCode());
        assertTrue(r.getDetail().contains("PROV_B no cuenta con el servicio OTP"));
        assertEquals(0, provB.calls);
    }

    @Test
    void centralConProveedorPreferidoSinServicioNoEnvia() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));
        catalog.deny("PROV_B", MessageType.OTP);

        DispatchResult r = service.dispatch(msg().toBuilder().preferredProvider("prov_b").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchResult.ERROR_SERVICIO_NO_SOPORTADO, r.getErrorCode());
        assertEquals("PROV_B", r.getProviderCode());
        assertEquals(0, provA.calls);
    }

    @Test
    void centralConProveedorPreferidoLoUsaPrimeroYLuegoLaRuta() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"));
        routing.byProvider.put("PROV_B", RoutePlan.empty("PROV_B", MessageType.OTP)); // sin ruta propia: usa sus cuentas de la central
        provB.script(500, 500, 500);
        provA.script(200);

        DispatchResult r = service.dispatch(msg().toBuilder().preferredProvider("PROV_B").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals(List.of("B_OTP#1:false", "B_OTP#2:false", "B_OTP#3:false", "A_OTP#1:true"), log.attempts);
    }

    @Test
    void cuentaSolicitadaDeProveedorSinServicioNoEnvia() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));
        accounts.add(account(2, "B_PRINCIPAL", "PROV_B"));
        catalog.deny("PROV_B", MessageType.OTP);

        DispatchResult r = service.dispatch(msg().toBuilder().accountCode("B_PRINCIPAL").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchResult.ERROR_SERVICIO_NO_SOPORTADO, r.getErrorCode());
        assertEquals("B_PRINCIPAL", r.getAccountCode());
        assertEquals(0, provA.calls + provB.calls);
    }

    @Test
    void fallbackSaltaProveedoresSinElMismoServicio() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"), step(3, "A_OTP2", "PROV_A"));
        catalog.deny("PROV_B", MessageType.OTP);
        provA.script(500, 500, 500, 200);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("A_OTP2", r.getAccountCode());
        assertEquals(0, provB.calls);
        assertEquals(List.of(1, 1, 1, 2), log.attemptOrders);
    }

    @Test
    void rutaSoloConProveedoresSinServicioEsServicioNoSoportado() {
        routing.plan = plan(step(1, "B_OTP", "PROV_B"));
        catalog.deny("PROV_B", MessageType.OTP);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchResult.ERROR_SERVICIO_NO_SOPORTADO, r.getErrorCode());
    }

    @Test
    void errorPermanentePasaDeInmediatoALaSiguienteCuenta() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"), step(2, "B_OTP", "PROV_B"));
        provA.permanent = true;
        provB.script(200);

        DispatchResult r = service.dispatch(msg(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals(1, provA.calls);
        assertEquals(2, r.getAttempts());
    }

    // ===================== fakes =====================

    static class FakeCatalog implements ProviderServiceCatalogPort {
        final Set<String> denied = new HashSet<>();

        void deny(String provider, MessageType type) {
            denied.add(provider.toUpperCase() + "|" + type);
        }

        @Override
        public boolean supports(String providerCode, MessageType type) {
            return providerCode != null && !denied.contains(providerCode.toUpperCase() + "|" + type);
        }

        @Override
        public Set<MessageType> servicesOf(String providerCode) {
            Set<MessageType> all = EnumSet.allOf(MessageType.class);
            all.removeIf(t -> !supports(providerCode, t));
            return all;
        }

        @Override
        public void refresh() {
        }
    }

    // ─── Ruta pedida en el request ───────────────────────────────────────────

    private void route(String ref, String provider, MessageType type, RouteStep... steps) {
        routing.routes.put(ref.toUpperCase(), RouteLookup.found(ref,
                RoutePlan.builder().routeId(99L).providerCode(provider).messageType(type).steps(List.of(steps)).build()));
    }

    @Test
    void rutaPedidaEjecutaSusPasosEnOrdenYNoLaAutomatica() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));
        route("OTP_RESPALDO", null, MessageType.OTP, step(1, "B_OTP", "PROV_B"), step(2, "A_OTP", "PROV_A"));
        provB.script(500, 500, 500);
        provA.script(200);

        DispatchResult r = service.dispatch(msg().toBuilder().routeReference("otp_respaldo").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("A_OTP", r.getAccountCode());
        assertEquals(4, r.getAttempts());
        assertEquals(List.of("B_OTP#1:false", "B_OTP#2:false", "B_OTP#3:false", "A_OTP#1:true"), log.attempts);
        assertEquals(List.of(1, 1, 1, 2), log.attemptOrders);
        assertTrue(!routing.resolved, "con ruta pedida no se resuelve por tipo de mensaje");
    }

    @Test
    void rutaPedidaTerminaEnElPrimerPasoExitoso() {
        route("7", null, MessageType.OTP, step(1, "B_OTP", "PROV_B"), step(2, "A_OTP", "PROV_A"));
        provB.script(200);

        DispatchResult r = service.dispatch(msg().toBuilder().routeReference("7").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals("B_OTP", r.getAccountCode());
        assertEquals(1, provB.calls);
        assertEquals(0, provA.calls);
    }

    @Test
    void rutaPedidaInexistente() {
        routing.plan = plan(step(1, "A_OTP", "PROV_A"));

        DispatchResult r = service.dispatch(msg().toBuilder().routeReference("NO_EXISTE").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.FALLIDO, r.getStatus());
        assertEquals(DispatchResult.ERROR_RUTA_NO_ENCONTRADA, r.getErrorCode());
        assertEquals(0, provA.calls);
        assertTrue(log.started);
    }

    @Test
    void rutaPedidaInactiva() {
        routing.routes.put("VIEJA", RouteLookup.inactive("VIEJA"));

        DispatchResult r = service.dispatch(msg().toBuilder().routeReference("vieja").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.FALLIDO, r.getStatus());
        assertEquals(DispatchResult.ERROR_RUTA_INACTIVA, r.getErrorCode());
        assertEquals(0, provA.calls + provB.calls);
    }

    @Test
    void rutaPedidaDeOtroTipo() {
        route("MASIVA", null, MessageType.MASIVO, step(1, "A_OTP", "PROV_A"));

        DispatchResult r = service.dispatch(msg().toBuilder().routeReference("MASIVA").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchResult.ERROR_RUTA_TIPO_NO_COINCIDE, r.getErrorCode());
        assertEquals(0, provA.calls);
    }

    @Test
    void rutaPedidaNoSeCombinaConCuentaNiProveedor() {
        route("R1", null, MessageType.OTP, step(1, "A_OTP", "PROV_A"));

        DispatchResult conCuenta = service.dispatch(msg().toBuilder().routeReference("R1").accountCode("A_OTP").build(),
                DispatchChannel.CENTRAL, null);
        DispatchResult conProveedor = service.dispatch(msg().toBuilder().routeReference("R1").preferredProvider("PROV_A").build(),
                DispatchChannel.CENTRAL, null);

        assertEquals(DispatchResult.ERROR_RUTA_NO_COMBINABLE, conCuenta.getErrorCode());
        assertEquals(DispatchResult.ERROR_RUTA_NO_COMBINABLE, conProveedor.getErrorCode());
        assertEquals(0, provA.calls);
    }

    @Test
    void rutaPedidaDeOtroProveedorEnEndpointDeProveedor() {
        route("R_B", "PROV_B", MessageType.OTP, step(1, "B_OTP", "PROV_B"));

        DispatchResult r = service.dispatch(msg().toBuilder().routeReference("R_B").build(), DispatchChannel.PROVEEDOR, "PROV_A");

        assertEquals(DispatchResult.ERROR_RUTA_NO_COMBINABLE, r.getErrorCode());
        assertEquals(0, provB.calls);
    }

    @Test
    void rutaPedidaOmiteProveedoresSinElServicio() {
        catalog.deny("PROV_B", MessageType.OTP);
        route("R1", null, MessageType.OTP, step(1, "B_OTP", "PROV_B"), step(2, "A_OTP", "PROV_A"));
        provA.script(200);

        DispatchResult r = service.dispatch(msg().toBuilder().routeReference("R1").build(), DispatchChannel.CENTRAL, null);

        assertEquals(DispatchStatus.ENVIADO, r.getStatus());
        assertEquals(0, provB.calls);
        assertEquals(List.of("A_OTP#1:true"), log.attempts);
    }

    private static SmsDispatch msg() {
        return SmsDispatch.builder().to("3157132338").text("Código 123456").type(MessageType.OTP).build();
    }

    private static RoutePlan plan(RouteStep... steps) {
        return RoutePlan.builder().messageType(MessageType.OTP).steps(List.of(steps)).build();
    }

    private static RouteStep step(int order, String account, String provider) {
        return RouteStep.builder().order(order).maxAttempts(3).waitMs(10)
                .account(account(order, account, provider))
                .build();
    }

    private static ProviderAccount account(long id, String code, String provider) {
        return ProviderAccount.builder().id(id).providerId(id).code(code).providerCode(provider).build();
    }

    static class FakeAccounts implements ProviderAccountQueryPort {
        final Map<String, ProviderAccount> byCode = new HashMap<>();
        String lastProvider;
        int queries;

        void add(ProviderAccount a) {
            byCode.put(a.getCode(), a);
        }

        @Override
        public Optional<ProviderAccount> findActive(String providerCode, String accountCode) {
            queries++;
            lastProvider = providerCode;
            return Optional.ofNullable(byCode.get(accountCode))
                    .filter(a -> providerCode == null || providerCode.equalsIgnoreCase(a.getProviderCode()));
        }

        @Override
        public Optional<ProviderAccount> findFirstActive(String providerCode) {
            return Optional.empty();
        }
    }

    static class FakeRouting implements RoutingPolicyPort {
        RoutePlan plan;
        /** Rutas propias por proveedor (si no hay entrada se devuelve {@link #plan}). */
        final Map<String, RoutePlan> byProvider = new HashMap<>();
        String lastProvider;
        boolean resolved;
        /** Rutas pedidas por código/id en el request. */
        final Map<String, RouteLookup> routes = new HashMap<>();

        @Override
        public RouteLookup findRoute(String reference) {
            return routes.getOrDefault(reference.toUpperCase(), RouteLookup.notFound(reference));
        }

        @Override
        public RoutePlan resolve(String providerCode, MessageType type) {
            resolved = true;
            lastProvider = providerCode;
            return providerCode != null && byProvider.containsKey(providerCode) ? byProvider.get(providerCode) : plan;
        }

        @Override
        public void refresh() {
        }
    }

    static class FakeLog implements SmsDispatchLogPort {
        boolean started;
        final List<String> attempts = new ArrayList<>();
        final List<Integer> attemptStatuses = new ArrayList<>();
        final List<Integer> attemptOrders = new ArrayList<>();
        final Map<String, DispatchResult> byKey = new HashMap<>();
        String finalAccount;

        @Override
        public UUID start(SmsDispatch message, DispatchChannel channel, String requestedProvider) {
            started = true;
            return message.getId();
        }

        @Override
        public void attempt(UUID envioId, RouteStep step, int attemptNumber, SendOutcome outcome) {
            attempts.add(step.getAccount().getCode() + "#" + attemptNumber + ":" + outcome.isSuccess());
            attemptStatuses.add(outcome.getHttpStatus());
            attemptOrders.add(step.getOrder());
        }

        @Override
        public void finish(DispatchResult result, RouteStep lastStep) {
            finalAccount = lastStep != null ? lastStep.getAccount().getCode() : null;
        }

        @Override
        public Optional<DispatchResult> findByIdempotencyKey(String key) {
            return Optional.ofNullable(byKey.get(key));
        }
    }

    static class ScriptedSender implements SmsSenderPort {
        private final String code;
        private final Deque<Integer> statuses = new ArrayDeque<>();
        int calls;
        boolean throwAlways;
        boolean permanent;

        ScriptedSender(String code) {
            this.code = code;
        }

        void script(Integer... values) {
            for (Integer v : values) {
                statuses.add(v == null ? -1 : v);
            }
        }

        @Override
        public String providerCode() {
            return code;
        }

        @Override
        public SendOutcome send(SmsDispatch message, ProviderAccount account) {
            calls++;
            if (throwAlways) {
                throw new IllegalStateException("boom");
            }
            if (permanent) {
                return SendOutcome.permanentFail(401, "credenciales", 5);
            }
            int s = statuses.isEmpty() ? 500 : statuses.poll();
            if (s == -1) {
                return SendOutcome.fail(null, "timeout", 5);
            }
            return s >= 200 && s < 300 ? SendOutcome.ok(s, 5) : SendOutcome.fail(s, "error " + s, 5);
        }
    }
}
