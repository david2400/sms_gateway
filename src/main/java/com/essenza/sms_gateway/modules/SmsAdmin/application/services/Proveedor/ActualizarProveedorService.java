package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ActualizarProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.ProveedorResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.ProveedorMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Actualiza un proveedor. Los campos opcionales que lleguen null conservan su valor actual. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class ActualizarProveedorService implements ActualizarProveedorUseCase {

    private final ProveedorAdminPort proveedores;
    private final AdminFinder finder;
    private final ProveedorMapper mapper;
    private final ProveedorResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = finder.proveedor(id);
        mapper.updateModel(request, proveedor);
        proveedor.setCodigo(AdminCodigos.normalizar(proveedor.getCodigo()));
        proveedores.findByCodigo(proveedor.getCodigo()).filter(p -> !p.getId().equals(id)).ifPresent(p -> {
            throw new AdminConflictException("El código " + p.getCodigo() + " ya lo usa el proveedor id " + p.getId());
        });
        Proveedor saved = proveedores.save(proveedor);
        log.info("[SMS-ADMIN] Proveedor {} actualizado", saved.getCodigo());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
