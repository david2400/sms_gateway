package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.ActualizarCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.CambioProveedorCuentaValidator;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.CuentaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class ActualizarCuentaService implements ActualizarCuentaUseCase {

    private final CuentaAdminPort cuentas;
    private final AdminFinder finder;
    private final CambioProveedorCuentaValidator cambioProveedorValidator;
    private final CuentaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public CuentaResponse actualizar(Long id, CuentaRequest request) {
        Cuenta actual = finder.cuenta(id);
        Proveedor proveedor = finder.proveedorReferenciado(request.getProveedorId());
        String codigo = AdminCodigos.normalizar(request.getCodigo());
        cuentas.findByCodigo(codigo).filter(c -> !c.getId().equals(id)).ifPresent(c -> {
            throw new AdminConflictException("El código " + codigo + " ya lo usa la cuenta id " + c.getId());
        });
        cambioProveedorValidator.validar(actual, proveedor);
        Cuenta saved = cuentas.save(actual.toBuilder()
                .proveedorId(proveedor.getId())
                .codigo(codigo)
                .usuario(AdminCodigos.blankToNull(request.getUsuario()))
                .secretRef(request.getSecretRef().trim())
                .remitente(AdminCodigos.blankToNull(request.getRemitente()))
                .parametros(request.getParametros() != null ? request.getParametros() : actual.getParametros())
                .activo(request.getActivo() != null ? request.getActivo() : actual.isActivo())
                .build());
        log.info("[SMS-ADMIN] Cuenta {} actualizada", codigo);
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
