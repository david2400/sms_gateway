package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.CrearCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.CuentaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.CredencialesCuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.CuentaMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

/** Crea una cuenta. La clave del request se guarda cifrada (o se usa secret_ref). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CrearCuentaService implements CrearCuentaUseCase {

    private final CuentaAdminPort cuentas;
    private final AdminFinder finder;
    private final CuentaMapper mapper;
    private final CredencialesCuenta credenciales;
    private final CuentaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public CuentaResponse crear(CuentaRequest request) {
        Proveedor proveedor = finder.proveedorReferenciado(request.getProveedorId());
        Cuenta cuenta = mapper.toModel(request);
        cuenta.setCodigo(AdminCodigos.normalizar(cuenta.getCodigo()));
        cuenta.setUsuario(AdminCodigos.blankToNull(cuenta.getUsuario()));
        credenciales.aplicar(cuenta, request.getClave());
        cuenta.setRemitente(AdminCodigos.blankToNull(cuenta.getRemitente()));
        if (cuenta.getParametros() == null) {
            cuenta.setParametros(new LinkedHashMap<>());
        }
        if (cuenta.getActivo() == null) {
            cuenta.setActivo(Boolean.TRUE);
        }
        cuentas.findByCodigo(cuenta.getCodigo()).ifPresent(c -> {
            throw new AdminConflictException("Ya existe la cuenta " + c.getCodigo() + " (id " + c.getId() + ")");
        });
        Cuenta saved = cuentas.save(cuenta);
        log.info("[SMS-ADMIN] Cuenta creada: {} ({})", saved.getCodigo(), proveedor.getCodigo());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
