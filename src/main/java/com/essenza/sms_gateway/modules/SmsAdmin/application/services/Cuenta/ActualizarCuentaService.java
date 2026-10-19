package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.ActualizarCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.CuentaResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.CambioProveedorCuentaValidator;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.CuentaMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Actualiza una cuenta. Los campos opcionales que lleguen null conservan su valor actual. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class ActualizarCuentaService implements ActualizarCuentaUseCase {

    private final CuentaAdminPort cuentas;
    private final AdminFinder finder;
    private final CambioProveedorCuentaValidator cambioProveedorValidator;
    private final CuentaMapper mapper;
    private final CuentaResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public CuentaResponse actualizar(Long id, CuentaRequest request) {
        Cuenta cuenta = finder.cuenta(id);
        Proveedor proveedor = finder.proveedorReferenciado(request.getProveedorId());
        cambioProveedorValidator.validar(cuenta, proveedor);
        mapper.updateModel(request, cuenta);
        cuenta.setCodigo(AdminCodigos.normalizar(cuenta.getCodigo()));
        cuenta.setUsuario(AdminCodigos.blankToNull(cuenta.getUsuario()));
        cuenta.setSecretRef(cuenta.getSecretRef().trim());
        cuenta.setRemitente(AdminCodigos.blankToNull(cuenta.getRemitente()));
        cuentas.findByCodigo(cuenta.getCodigo()).filter(c -> !c.getId().equals(id)).ifPresent(c -> {
            throw new AdminConflictException("El código " + c.getCodigo() + " ya lo usa la cuenta id " + c.getId());
        });
        Cuenta saved = cuentas.save(cuenta);
        log.info("[SMS-ADMIN] Cuenta {} actualizada", saved.getCodigo());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
