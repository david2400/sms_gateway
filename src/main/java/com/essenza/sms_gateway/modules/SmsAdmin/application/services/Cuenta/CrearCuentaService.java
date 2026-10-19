package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.CrearCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.CuentaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class CrearCuentaService implements CrearCuentaUseCase {

    private final CuentaAdminPort cuentas;
    private final AdminFinder finder;
    private final CuentaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public CuentaResponse crear(CuentaRequest request) {
        Proveedor proveedor = finder.proveedorReferenciado(request.getProveedorId());
        String codigo = AdminCodigos.normalizar(request.getCodigo());
        cuentas.findByCodigo(codigo).ifPresent(c -> {
            throw new AdminConflictException("Ya existe la cuenta " + codigo + " (id " + c.getId() + ")");
        });
        Cuenta saved = cuentas.save(Cuenta.builder()
                .proveedorId(proveedor.getId())
                .codigo(codigo)
                .usuario(AdminCodigos.blankToNull(request.getUsuario()))
                .secretRef(request.getSecretRef().trim())
                .remitente(AdminCodigos.blankToNull(request.getRemitente()))
                .parametros(request.getParametros() != null ? request.getParametros() : new LinkedHashMap<>())
                .activo(request.getActivo() == null || request.getActivo())
                .build());
        log.info("[SMS-ADMIN] Cuenta creada: {} ({})", codigo, proveedor.getCodigo());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
