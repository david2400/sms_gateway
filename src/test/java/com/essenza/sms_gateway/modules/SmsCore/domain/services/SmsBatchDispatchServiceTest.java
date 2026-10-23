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
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsBatchSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Masivo: lotes nativos, reintentos por lote, fallback del lote completo a la siguiente cuenta
 * (solo proveedores con el mismo servicio) y envío uno a uno para proveedores sin API masiva.
 */
class SmsBatchDispatchServiceTest {

    private SmsDispatchServiceTest.FakeRouting routing;
    private SmsDispatchServiceTest.FakeLog log;
    private SmsDispatchServiceTest.FakeCatalog catalog;
    private BatchSender bulkA;
    private SmsDispatchServiceTest.ScriptedSender provA;
    private SmsDispatchServiceTest.ScriptedSender provB;
    private SmsBatchDispatchService service;

    @BeforeEach
    void setUp() {
        routing = new SmsDispatchServiceTest.FakeRouting();
        log = new SmsDispatchServiceTest.FakeLog();
        catalog = new SmsDispatchServiceTest.FakeCatalog();
        bulkA = new BatchSender("PROV_A", 500);
        provA = new SmsDispatchServiceTest.ScriptedSender("PROV_A");
        provB = new SmsDispatchServiceTest.ScriptedSender("PROV_B");
        service = new SmsBatchDispatchService(
                new DispatchPlanner(routing, new SmsDispatchServiceTest.FakeAccounts(), catalog),
                log, List.of(provA, provB), List.of(bulkA)) {
            @Override
            protected boolean pause(long millis) {
                return true;
            }
        };
    }

    @Test
    void parteEnLotesDelTamanoDelProveedor() {
        routing.plan = plan(step(1, "A_MASIVO", "PROV_A"));
        bulkA.script(200, 200, 200);

        List<DispatchResult> r = service.dispatch(messages(1200), MessageType.MASIVO, DispatchChannel.CENTRAL,
                null, null, null, "L1");

        assertEquals(List.of(500, 500, 200), bulkA.sizes);
        assertEquals(List.of("L1-1", "L1-2", "L1-3"), bulkA.ids);
        assertTrue(r.stream().allMatch(DispatchResult::isSent));
        assertEquals(0, provA.calls);
    }

    @Test
    void loteQueAgotaIntentosPasaCompletoALaSiguienteCuenta() {
        routing.plan = plan(step(1, "A_MASIVO", "PROV_A"), step(2, "B_MASIVO", "PROV_B"));
        bulkA.script(503, 503, 503);
        provB.script(200, 200, 200);

        List<DispatchResult> r = service.dispatch(messages(3), MessageType.MASIVO, DispatchChannel.CENTRAL,
                null, null, null, "L2");

        assertEquals(3, bulkA.sizes.size());
        assertEquals(3, provB.calls);
        r.forEach(x -> {
            assertEquals(DispatchStatus.ENVIADO, x.getStatus());
            assertEquals("B_MASIVO", x.getAccountCode());
            assertEquals(4, x.getAttempts());
        });
    }

    @Test
    void errorPermanenteDelLoteNoSeReintentaConLaMismaCuenta() {
        routing.plan = plan(step(1, "A_MASIVO", "PROV_A"), step(2, "B_MASIVO", "PROV_B"));
        bulkA.permanent = true;
        provB.script(200, 200);

        List<DispatchResult> r = service.dispatch(messages(2), MessageType.MASIVO, DispatchChannel.CENTRAL,
                null, null, null, "L3");

        assertEquals(1, bulkA.sizes.size());
        assertTrue(r.stream().allMatch(DispatchResult::isSent));
    }

    @Test
    void fallbackSoloPorProveedoresConElMismoServicio() {
        routing.plan = plan(step(1, "A_MASIVO", "PROV_A"), step(2, "B_MASIVO", "PROV_B"));
        catalog.deny("PROV_B", MessageType.MASIVO);
        bulkA.script(500, 500, 500);

        List<DispatchResult> r = service.dispatch(messages(2), MessageType.MASIVO, DispatchChannel.CENTRAL,
                null, null, null, "L4");

        assertEquals(0, provB.calls);
        r.forEach(x -> assertEquals(DispatchResult.ERROR_TODAS_FALLARON, x.getErrorCode()));
    }

    @Test
    void proveedorSinServicioRechazaTodoElMasivo() {
        routing.plan = plan(step(1, "B_MASIVO", "PROV_B"));
        catalog.deny("PROV_B", MessageType.MASIVO);

        List<DispatchResult> r = service.dispatch(messages(2), MessageType.MASIVO, DispatchChannel.PROVEEDOR,
                "PROV_B", null, null, "L5");

        r.forEach(x -> assertEquals(DispatchResult.ERROR_SERVICIO_NO_SOPORTADO, x.getErrorCode()));
        assertEquals(0, provB.calls);
    }

    @Test
    void destinoInvalidoNoBloqueaAlResto() {
        routing.plan = plan(step(1, "A_MASIVO", "PROV_A"));
        bulkA.script(200);
        List<SmsDispatch> msgs = new ArrayList<>(messages(2));
        msgs.add(SmsDispatch.builder().to("123").text("x").type(MessageType.MASIVO).build());

        List<DispatchResult> r = service.dispatch(msgs, MessageType.MASIVO, DispatchChannel.CENTRAL, null, null, null, "L6");

        assertEquals(List.of(2), bulkA.sizes);
        assertEquals(DispatchResult.ERROR_VALIDACION, r.get(2).getErrorCode());
        assertTrue(r.get(0).isSent() && r.get(1).isSent());
    }

    // ---------------- fakes ----------------

    private static List<SmsDispatch> messages(int n) {
        return IntStream.range(0, n).mapToObj(i -> SmsDispatch.builder()
                .to("31571" + String.format("%05d", i)).text("Promo " + (i % 2)).type(MessageType.MASIVO).build()).toList();
    }

    private static RoutePlan plan(RouteStep... steps) {
        return RoutePlan.builder().messageType(MessageType.MASIVO).steps(List.of(steps)).build();
    }

    private static RouteStep step(int order, String account, String provider) {
        return RouteStep.builder().order(order).maxAttempts(3).waitMs(10)
                .account(ProviderAccount.builder().id((long) order).providerId((long) order).code(account).providerCode(provider).build())
                .build();
    }

    static class BatchSender implements SmsBatchSenderPort {
        private final String code;
        private final int max;
        private final Deque<Integer> statuses = new ArrayDeque<>();
        final List<Integer> sizes = new ArrayList<>();
        final List<String> ids = new ArrayList<>();
        boolean permanent;

        BatchSender(String code, int max) {
            this.code = code;
            this.max = max;
        }

        void script(Integer... values) {
            statuses.addAll(List.of(values));
        }

        @Override
        public String providerCode() {
            return code;
        }

        @Override
        public int maxBatchSize(ProviderAccount account) {
            return max;
        }

        @Override
        public SendOutcome sendBatch(List<SmsDispatch> messages, ProviderAccount account, String batchId) {
            sizes.add(messages.size());
            ids.add(batchId);
            if (permanent) {
                return SendOutcome.permanentFail(200, "Status 1", 5);
            }
            int s = statuses.isEmpty() ? 500 : statuses.poll();
            return s >= 200 && s < 300 ? SendOutcome.ok(s, 5) : SendOutcome.fail(s, "error " + s, 5);
        }
    }
}
