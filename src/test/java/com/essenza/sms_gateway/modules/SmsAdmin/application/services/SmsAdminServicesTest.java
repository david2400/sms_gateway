package com.essenza.sms_gateway.modules.SmsAdmin.application.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.PasoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaEfectivaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorCatalogPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta.ActualizarCuentaService;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta.CrearCuentaService;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta.ObtenerCuentaService;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor.CrearProveedorService;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor.ObtenerProveedorService;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta.ConsultarRutaEfectivaService;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta.CrearRutaService;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta.ReemplazarPasosRutaService;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.CambioProveedorCuentaValidator;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.PasoRutaValidator;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.CuentaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.ProveedorResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.RutaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.CuentaMapperImpl;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.ParametrosJsonMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.ProveedorMapperImpl;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaMapperImpl;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaEfectivaMapperImpl;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaPasoMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaPasoMapperImpl;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SecretCipherPort;
import com.essenza.sms_gateway.modules.SmsCore.shared.crypto.AesGcmCipher;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.CredencialesCuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas de administración con puertos en memoria.
 */
class SmsAdminServicesTest {

    private MemProveedores proveedores;
    private MemCuentas cuentas;
    private TestCipher cipher;
    private MemRutas rutas;
    private MemPasos pasos;
    private AtomicInteger refreshes;
    private RoutePlan planEfectivo;

    private CrearProveedorService crearProveedor;
    private ObtenerProveedorService obtenerProveedor;
    private CrearCuentaService crearCuenta;
    private ObtenerCuentaService obtenerCuenta;
    private ActualizarCuentaService actualizarCuenta;
    private CrearRutaService crearRuta;
    private ReemplazarPasosRutaService reemplazarPasos;
    private ConsultarRutaEfectivaService rutaEfectiva;

    private Long inalambria;
    private Long go4;
    private Long cuentaOtp;
    private Long cuentaGo4;

    @BeforeEach
    void setUp() {
        proveedores = new MemProveedores();
        cuentas = new MemCuentas(proveedores);
        cipher = new TestCipher();
        rutas = new MemRutas(proveedores);
        pasos = new MemPasos(cuentas);
        refreshes = new AtomicInteger();
        RoutingCacheAdminPort cache = refreshes::incrementAndGet;
        ProveedorCatalogPort catalog = () -> Set.of("INALAMBRIA", "GO4CLIENTS");
        RoutingPolicyPort routing = new RoutingPolicyPort() {
            @Override
            public RoutePlan resolve(String providerCode, MessageType type) {
                return planEfectivo;
            }

            @Override
            public void refresh() {
            }
        };

        AdminFinder finder = new AdminFinder(proveedores, cuentas, rutas);
        ProveedorMapperImpl proveedorMapper = new ProveedorMapperImpl();
        CuentaMapperImpl cuentaMapper = new CuentaMapperImpl(new ParametrosJsonMapper());
        RutaPasoMapper pasoMapper = new RutaPasoMapperImpl();
        RutaMapperImpl rutaMapper = new RutaMapperImpl(pasoMapper);
        ProveedorResponseAssembler proveedorAssembler = new ProveedorResponseAssembler(proveedorMapper, catalog, cuentas);
        CuentaResponseAssembler cuentaAssembler = new CuentaResponseAssembler(cuentaMapper, ref -> "INALAMBRIA_OTP_PASSWORD".equals(ref), pasos);
        RutaResponseAssembler rutaAssembler = new RutaResponseAssembler(rutaMapper, pasos);
        PasoRutaValidator pasoValidator = new PasoRutaValidator(finder);

        crearProveedor = new CrearProveedorService(proveedores, proveedorMapper, proveedorAssembler, cache);
        obtenerProveedor = new ObtenerProveedorService(finder, proveedorAssembler);
        CredencialesCuenta credenciales = new CredencialesCuenta(cipher);
        crearCuenta = new CrearCuentaService(cuentas, finder, cuentaMapper, credenciales, cuentaAssembler, cache);
        obtenerCuenta = new ObtenerCuentaService(finder, cuentaAssembler);
        actualizarCuenta = new ActualizarCuentaService(cuentas, finder, new CambioProveedorCuentaValidator(pasos, rutas), cuentaMapper, credenciales, cuentaAssembler, cache);
        crearRuta = new CrearRutaService(rutas, pasos, finder, pasoValidator, rutaMapper, pasoMapper, rutaAssembler, cache);
        reemplazarPasos = new ReemplazarPasosRutaService(pasos, finder, pasoValidator, pasoMapper, rutaAssembler, cache);
        rutaEfectiva = new ConsultarRutaEfectivaService(routing, catalog, new RutaEfectivaMapperImpl());

        inalambria = crearProveedor.crear(proveedor("inalambria")).getId();
        go4 = crearProveedor.crear(proveedor("GO4CLIENTS")).getId();
        cuentaOtp = crearCuenta.crear(cuenta(inalambria, "inalambria_otp", "INALAMBRIA_OTP_PASSWORD")).getId();
        cuentaGo4 = crearCuenta.crear(cuenta(go4, "GO4CLIENTS_PRINCIPAL", "GO4CLIENTS_API_SECRET")).getId();
        refreshes.set(0);
    }

    @Test
    void proveedorDuplicadoEsConflicto() {
        assertThrows(AdminConflictException.class, () -> crearProveedor.crear(proveedor("INALAMBRIA")));
    }

    @Test
    void codigosSeGuardanEnMayusculasYSeMarcaImplementado() {
        var p = obtenerProveedor.obtener(inalambria);
        assertEquals("INALAMBRIA", p.getCodigo());
        assertTrue(p.isImplementado());
        assertEquals(1, p.getCuentas());
        assertFalse(crearProveedor.crear(proveedor("AWS_SNS")).isImplementado());
    }

    // ================= clave cifrada =================

    @Test
    void claveSeGuardaCifradaYLaRespuestaNoLaExpone() {
        CuentaRequest r = cuenta(inalambria, "inalambria_masivo", null);
        r.setClave("Secreto,123");
        var resp = crearCuenta.crear(r);

        Cuenta guardada = cuentas.data.get(resp.getId());
        assertTrue(guardada.getClaveCifrada().startsWith(AesGcmCipher.PREFIX));
        assertFalse(guardada.getClaveCifrada().contains("Secreto"));
        assertEquals("Secreto,123", cipher.decrypt(guardada.getClaveCifrada()));
        assertTrue(resp.isClaveConfigurada());
        assertNull(guardada.getSecretRef());
    }

    @Test
    void actualizarSinClaveConservaLaCifrada() {
        CuentaRequest r = cuenta(inalambria, "INALAMBRIA_HD", null);
        r.setClave("Mv.Prueba");
        Long id = crearCuenta.crear(r).getId();
        String antes = cuentas.data.get(id).getClaveCifrada();

        CuentaRequest u = cuenta(inalambria, "INALAMBRIA_HD", null);
        u.setUsuario("otro");
        actualizarCuenta.actualizar(id, u);

        assertEquals(antes, cuentas.data.get(id).getClaveCifrada());
        assertEquals("otro", cuentas.data.get(id).getUsuario());
    }

    @Test
    void actualizarConClaveNuevaLaReemplaza() {
        CuentaRequest r = cuenta(inalambria, "INALAMBRIA_TPL", null);
        r.setClave("vieja");
        Long id = crearCuenta.crear(r).getId();

        CuentaRequest u = cuenta(inalambria, "INALAMBRIA_TPL", null);
        u.setClave("nueva");
        actualizarCuenta.actualizar(id, u);

        assertEquals("nueva", cipher.decrypt(cuentas.data.get(id).getClaveCifrada()));
    }

    @Test
    void cuentaSinClaveNiSecretRefEsInvalida() {
        assertThrows(AdminValidationException.class, () -> crearCuenta.crear(cuenta(inalambria, "SIN_CLAVE", null)));
    }

    @Test
    void claveSinLlaveDeCifradoEsInvalida() {
        cipher.configured = false;
        CuentaRequest r = cuenta(inalambria, "SIN_LLAVE", null);
        r.setClave("x");
        assertThrows(AdminValidationException.class, () -> crearCuenta.crear(r));
    }

    @Test
    void cuentaIndicaSiElSecretoEstaDefinido() {
        assertTrue(obtenerCuenta.obtener(cuentaOtp).isSecretDefinido());
        assertFalse(obtenerCuenta.obtener(cuentaGo4).isSecretDefinido());
        assertEquals("INALAMBRIA_OTP", obtenerCuenta.obtener(cuentaOtp).getCodigo());
    }

    @Test
    void rutaCentralPuedeMezclarProveedoresYRefrescaCache() {
        RutaResponse r = crearRuta.crear(ruta(null, MessageType.OTP, List.of(paso(1, cuentaOtp), paso(2, cuentaGo4))));

        assertEquals("CENTRAL", r.getProveedorCodigo());
        assertEquals(2, r.getPasos().size());
        assertEquals(3, r.getPasos().get(0).getMaxIntentos());
        assertEquals(1, refreshes.get());
    }

    @Test
    void rutaDeProveedorNoAceptaCuentasDeOtro() {
        assertThrows(AdminValidationException.class, () -> crearRuta.crear(ruta(inalambria, MessageType.OTP, List.of(paso(1, cuentaOtp), paso(2, cuentaGo4)))));
    }

    @Test
    void rutaDuplicadaEsConflicto() {
        crearRuta.crear(ruta(inalambria, MessageType.OTP, null));
        assertThrows(AdminConflictException.class, () ->
                crearRuta.crear(ruta(inalambria, MessageType.OTP, null)));
    }

    @Test
    void ordenOCuentaRepetidosSonInvalidos() {
        Long id = crearRuta.crear(ruta(null, MessageType.NORMAL, null)).getId();
        assertThrows(AdminValidationException.class, () -> reemplazarPasos.reemplazarPasos(id, List.of(paso(1, cuentaOtp), paso(1, cuentaGo4))));
        assertThrows(AdminValidationException.class, () -> reemplazarPasos.reemplazarPasos(id, List.of(paso(1, cuentaOtp), paso(2, cuentaOtp))));
    }

    @Test
    void reemplazarPasosCambiaElOrden() {
        Long id = crearRuta.crear(ruta(null, MessageType.OTP, List.of(paso(1, cuentaOtp), paso(2, cuentaGo4)))).getId();
        RutaResponse r = reemplazarPasos.reemplazarPasos(id, List.of(paso(1, cuentaGo4), paso(2, cuentaOtp)));
        assertEquals("GO4CLIENTS_PRINCIPAL", r.getPasos().get(0).getCuentaCodigo());
    }

    @Test
    void noSeCambiaDeProveedorUnaCuentaUsadaEnRutaDeProveedor() {
        crearRuta.crear(ruta(inalambria, MessageType.OTP, List.of(paso(1, cuentaOtp))));
        assertThrows(AdminConflictException.class, () ->
                actualizarCuenta.actualizar(cuentaOtp, cuenta(go4, "INALAMBRIA_OTP", "INALAMBRIA_OTP_PASSWORD")));
    }

    @Test
    void rutaEfectivaAdvierteCuentasSinCredenciales() {
        planEfectivo = RoutePlan.builder().messageType(MessageType.OTP).steps(List.of(
                RouteStep.builder().order(1).maxAttempts(3).waitMs(500).account(ProviderAccount.builder()
                        .code("INALAMBRIA_OTP").providerCode("INALAMBRIA").username("u").password("p").build()).build(),
                RouteStep.builder().order(2).maxAttempts(3).waitMs(500).account(ProviderAccount.builder()
                        .code("AWS_1").providerCode("AWS_SNS").username("u").build()).build())).build();

        RutaEfectivaResponse r = rutaEfectiva.efectiva(null, MessageType.OTP);

        assertTrue(r.isConfigurada());
        assertEquals("CENTRAL", r.getProveedor());
        assertTrue(r.getPasos().get(0).isCredencialesOk());
        assertFalse(r.getPasos().get(1).isCredencialesOk());
        assertFalse(r.getPasos().get(1).isImplementado());
        assertEquals(2, r.getAdvertencias().size());
    }

    // ===================== helpers y puertos en memoria =====================

    private static ProveedorRequest proveedor(String codigo) {
        ProveedorRequest r = new ProveedorRequest();
        r.setCodigo(codigo);
        r.setNombre(codigo);
        r.setBaseUrl("https://x");
        return r;
    }

    private static CuentaRequest cuenta(Long proveedorId, String codigo, String secretRef) {
        CuentaRequest r = new CuentaRequest();
        r.setProveedorId(proveedorId);
        r.setCodigo(codigo);
        r.setUsuario("u");
        r.setSecretRef(secretRef);
        return r;
    }

    private static RutaRequest ruta(Long proveedorId, MessageType tipo, List<PasoRequest> pasos) {
        RutaRequest r = new RutaRequest();
        r.setProveedorId(proveedorId);
        r.setTipoMensaje(tipo);
        r.setPasos(pasos);
        return r;
    }

    private static PasoRequest paso(int orden, Long cuentaId) {
        PasoRequest r = new PasoRequest();
        r.setOrden(orden);
        r.setCuentaId(cuentaId);
        return r;
    }

    /** Cifrador real AES-GCM con una llave de prueba. */
    static class TestCipher implements SecretCipherPort {
        final AesGcmCipher aes = new AesGcmCipher(AesGcmCipher.generateKey());
        boolean configured = true;

        public boolean isConfigured() { return configured; }
        public String encrypt(String plain) { return aes.encrypt(plain); }
        public String decrypt(String text) { return aes.decrypt(text); }
    }

    static class MemProveedores implements ProveedorAdminPort {
        final Map<Long, Proveedor> data = new LinkedHashMap<>();
        final AtomicLong seq = new AtomicLong();

        public List<Proveedor> findAll() { return new ArrayList<>(data.values()); }
        public Optional<Proveedor> findById(Long id) { return Optional.ofNullable(data.get(id)); }
        public Optional<Proveedor> findByCodigo(String c) { return data.values().stream().filter(p -> p.getCodigo().equals(c)).findFirst(); }
        public Proveedor save(Proveedor p) {
            if (p.getId() == null) {
                p.setId(seq.incrementAndGet());
            }
            Proveedor s = p;
            data.put(s.getId(), s);
            return s;
        }
    }

    static class MemCuentas implements CuentaAdminPort {
        final Map<Long, Cuenta> data = new LinkedHashMap<>();
        final AtomicLong seq = new AtomicLong();
        final MemProveedores proveedores;

        MemCuentas(MemProveedores proveedores) { this.proveedores = proveedores; }

        public List<Cuenta> findAll(Long pid) { return data.values().stream().filter(c -> pid == null || pid.equals(c.getProveedorId())).toList(); }
        public Optional<Cuenta> findById(Long id) { return Optional.ofNullable(data.get(id)); }
        public Optional<Cuenta> findByCodigo(String c) { return data.values().stream().filter(x -> x.getCodigo().equals(c)).findFirst(); }
        public Cuenta save(Cuenta c) {
            if (c.getId() == null) {
                c.setId(seq.incrementAndGet());
            }
            c.setProveedorCodigo(proveedores.data.get(c.getProveedorId()).getCodigo());
            Cuenta s = c;
            data.put(s.getId(), s);
            return s;
        }
    }

    static class MemRutas implements RutaAdminPort {
        final Map<Long, Ruta> data = new LinkedHashMap<>();
        final AtomicLong seq = new AtomicLong();
        final MemProveedores proveedores;

        MemRutas(MemProveedores proveedores) { this.proveedores = proveedores; }

        public List<Ruta> findAll(Long pid, boolean central, String tipo) { return new ArrayList<>(data.values()); }
        public Optional<Ruta> findById(Long id) { return Optional.ofNullable(data.get(id)); }
        public Optional<Ruta> findByProveedorAndTipo(Long pid, String tipo) {
            return data.values().stream().filter(r -> Objects.equals(pid, r.getProveedorId()) && r.getTipoMensaje().equals(tipo)).findFirst();
        }
        public Ruta save(Ruta r) {
            if (r.getId() == null) {
                r.setId(seq.incrementAndGet());
            }
            r.setProveedorCodigo(r.getProveedorId() == null ? null : proveedores.data.get(r.getProveedorId()).getCodigo());
            Ruta s = r;
            data.put(s.getId(), s);
            return s;
        }
        public void delete(Long id) { data.remove(id); }
    }

    static class MemPasos implements RutaPasoAdminPort {
        final Map<Long, List<Paso>> data = new LinkedHashMap<>();
        final MemCuentas cuentas;

        MemPasos(MemCuentas cuentas) { this.cuentas = cuentas; }

        public List<Paso> findByRuta(Long id) { return data.getOrDefault(id, List.of()); }
        public List<Paso> replace(Long id, List<Paso> nuevos) {
            nuevos.forEach(p -> p.setCuentaCodigo(cuentas.data.get(p.getCuentaId()).getCodigo()));
            data.put(id, List.copyOf(nuevos));
            return data.get(id);
        }
        public void deleteByRuta(Long id) { data.remove(id); }
        public List<Long> rutasQueUsanCuenta(Long cid) {
            return data.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(p -> p.getCuentaId().equals(cid))).map(Map.Entry::getKey).toList();
        }
    }
}
