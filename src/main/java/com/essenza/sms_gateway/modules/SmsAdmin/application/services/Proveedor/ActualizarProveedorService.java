package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ActualizarProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.ProveedorResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Actualiza un proveedor. Los campos opcionales vacíos conservan su valor actual. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class ActualizarProveedorService implements ActualizarProveedorUseCase {

    private final ProveedorAdminPort proveedores;
    private final AdminFinder finder;
    private final ProveedorResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor actual = finder.proveedor(id);
        String codigo = AdminCodigos.normalizar(request.getCodigo());
        proveedores.findByCodigo(codigo).filter(p -> !p.getId().equals(id)).ifPresent(p -> {
            throw new AdminConflictException("El código " + codigo + " ya lo usa el proveedor id " + p.getId());
        });
        Proveedor saved = proveedores.save(actual.toBuilder()
                .codigo(codigo)
                .nombre(request.getNombre().trim())
                .baseUrl(request.getBaseUrl().trim())
                .connectTimeoutMs(request.getConnectTimeoutMs() != null ? request.getConnectTimeoutMs() : actual.getConnectTimeoutMs())
                .readTimeoutMs(request.getReadTimeoutMs() != null ? request.getReadTimeoutMs() : actual.getReadTimeoutMs())
                .activo(request.getActivo() != null ? request.getActivo() : actual.isActivo())
                .build());
        log.info("[SMS-ADMIN] Proveedor {} actualizado", codigo);
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
