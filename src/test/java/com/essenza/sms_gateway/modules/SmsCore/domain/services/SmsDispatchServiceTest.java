package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsDispatchLogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private ScriptedSender provA;
    private ScriptedSender provB;
    private SmsDispatchService service;

    @BeforeEach
    void setUp() {
        routing = new FakeRouting();
        log = new FakeLog();
        provA = new ScriptedSender("PROV_A");
        provB = new ScriptedSender("PROV_B");
        service = new SmsDispatchService(routing, log, List.of(provA, provB)) {
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

    // ===================== fakes =====================

    private static SmsDispatch msg() {
        return SmsDispatch.builder().to("3157132338").text("Código 123456").type(MessageType.OTP).build();
    }

    private static RoutePlan plan(RouteStep... steps) {
        return RoutePlan.builder().messageType(MessageType.OTP).steps(List.of(steps)).build();
    }

    private static RouteStep step(int order, String account, String provider) {
        return RouteStep.builder().order(order).maxAttempts(3).waitMs(10)
                .account(ProviderAccount.builder().id((long) order).providerId((long) order)
                        .code(account).providerCode(provider).build())
                .build();
    }

    static class FakeRouting implements RoutingPolicyPort {
        RoutePlan plan;
        String lastProvider;

        @Override
        public RoutePlan resolve(String providerCode, MessageType type) {
            lastProvider = providerCode;
            return plan;
        }

        @Override
        public void refresh() {
        }
    }

    static class FakeLog implements SmsDispatchLogPort {
        boolean started;
        final List<String> attempts = new ArrayList<>();
        final List<Integer> attemptStatuses = new ArrayList<>();
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
            int s = statuses.isEmpty() ? 500 : statuses.poll();
            if (s == -1) {
                return SendOutcome.fail(null, "timeout", 5);
            }
            return s >= 200 && s < 300 ? SendOutcome.ok(s, 5) : SendOutcome.fail(s, "error " + s, 5);
        }
    }
}
