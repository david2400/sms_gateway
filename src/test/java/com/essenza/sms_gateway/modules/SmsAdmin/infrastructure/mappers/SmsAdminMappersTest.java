package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.mappers;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.mappers.CuentaDtoMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.mappers.CuentaDtoMapperImpl;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.mappers.ProveedorDtoMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.mappers.ProveedorDtoMapperImpl;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.CuentaPersistenceMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.CuentaPersistenceMapperImpl;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.ParametrosJsonMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminCuentaEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminProveedorEntity;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica las implementaciones generadas por MapStruct (un mapper por responsabilidad). */
class SmsAdminMappersTest {

    private final ParametrosJsonMapper parametros = new ParametrosJsonMapper();
    private final CuentaPersistenceMapper cuentaPersistence = new CuentaPersistenceMapperImpl(parametros);
    private final ProveedorDtoMapper proveedorDto = new ProveedorDtoMapperImpl();
    private final CuentaDtoMapper cuentaDto = new CuentaDtoMapperImpl();

    @Test
    void cuentaEntidadAModeloYDeVuelta() {
        AdminProveedorEntity proveedor = new AdminProveedorEntity();
        proveedor.setIdSmsProveedor(7L);
        proveedor.setCodigo("INALAMBRIA");
        AdminCuentaEntity entity = new AdminCuentaEntity();
        entity.setIdSmsCuenta(3L);
        entity.setProveedor(proveedor);
        entity.setCodigo("INALAMBRIA_OTP");
        entity.setSecretRef("INALAMBRIA_OTP_PASSWORD");
        entity.setParametrosJson("{\"type\":1,\"messagePattern\":\"0\"}");
        entity.setActivo(true);

        Cuenta cuenta = cuentaPersistence.toModel(entity);
        assertEquals(7L, cuenta.getProveedorId());
        assertEquals("INALAMBRIA", cuenta.getProveedorCodigo());
        assertEquals(1, cuenta.getParametros().get("type"));

        AdminCuentaEntity destino = new AdminCuentaEntity();
        cuentaPersistence.updateEntity(cuenta, destino);
        assertNull(destino.getIdSmsCuenta());
        assertNull(destino.getProveedor());
        assertEquals("{\"type\":1,\"messagePattern\":\"0\"}", destino.getParametrosJson());
    }

    @Test
    void parametrosAnidadosSonInvalidos() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("x", Map.of("y", 1));
        assertThrows(AdminValidationException.class, () -> parametros.mapToJson(params));
    }

    @Test
    void actualizarNoPisaConNulls() {
        Proveedor actual = new Proveedor(1L, "INALAMBRIA", "Inalambria", "https://a", 3000, 10000, true, null);
        ProveedorRequest request = new ProveedorRequest();
        request.setCodigo("INALAMBRIA");
        request.setNombre("Inalambria SA");
        request.setBaseUrl("https://b");

        proveedorDto.updateModel(request, actual);

        assertEquals("Inalambria SA", actual.getNombre());
        assertEquals("https://b", actual.getBaseUrl());
        assertEquals(3000, actual.getConnectTimeoutMs());
        assertTrue(actual.getActivo());
        assertEquals(1L, actual.getId());
    }

    @Test
    void respuestaDeCuentaIncluyeDatosCalculados() {
        CuentaRequest request = new CuentaRequest();
        request.setProveedorId(1L);
        request.setCodigo("X");
        request.setSecretRef("X_PASSWORD");
        Cuenta cuenta = cuentaDto.toModel(request);
        cuenta.setId(9L);

        CuentaResponse r = cuentaDto.toResponse(cuenta, true, List.of(4L, 5L));

        assertEquals(9L, r.getId());
        assertTrue(r.isSecretDefinido());
        assertEquals(List.of(4L, 5L), r.getRutas());
    }
}
