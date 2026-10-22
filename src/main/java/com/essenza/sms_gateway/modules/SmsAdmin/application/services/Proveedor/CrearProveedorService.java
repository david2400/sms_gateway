package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.CrearProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.services.support.ProveedorResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.ProveedorMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crea un proveedor. Timeouts por defecto: 2000 ms conexión, 5000 ms lectura. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CrearProveedorService implements CrearProveedorUseCase {

    static final int DEFAULT_CONNECT_TIMEOUT_MS = 2000;
    static final int DEFAULT_READ_TIMEOUT_MS = 5000;

    private final ProveedorAdminPort proveedores;
    private final ProveedorMapper mapper;
    private final ProveedorResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public ProveedorResponse crear(ProveedorRequest request) {
        Proveedor proveedor = mapper.toModel(request);
        proveedor.setCodigo(AdminCodigos.normalizar(proveedor.getCodigo()));
        proveedor.setNombre(proveedor.getNombre().trim());
        proveedor.setBaseUrl(proveedor.getBaseUrl().trim());
        if (proveedor.getConnectTimeoutMs() == null) {
            proveedor.setConnectTimeoutMs(DEFAULT_CONNECT_TIMEOUT_MS);
        }
        if (proveedor.getReadTimeoutMs() == null) {
            proveedor.setReadTimeoutMs(DEFAULT_READ_TIMEOUT_MS);
        }
        if (proveedor.getEliminado() == null) {
            proveedor.setEliminado(Boolean.FALSE);
        }
        proveedores.findByCodigo(proveedor.getCodigo()).ifPresent(p -> {
            throw new AdminConflictException("Ya existe el proveedor " + p.getCodigo() + " (id " + p.getId() + ")");
        });
        Proveedor saved = proveedores.save(proveedor);
        log.info("[SMS-ADMIN] Proveedor creado: {} (id {})", saved.getCodigo(), saved.getId());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
