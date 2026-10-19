package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.CrearProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.ProveedorResponseAssembler;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crea un proveedor. Timeouts por defecto: 2000 ms conexión, 5000 ms lectura. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "maestraTransactionManager")
public class CrearProveedorService implements CrearProveedorUseCase {

    static final int DEFAULT_CONNECT_TIMEOUT_MS = 2000;
    static final int DEFAULT_READ_TIMEOUT_MS = 5000;

    private final ProveedorAdminPort proveedores;
    private final ProveedorResponseAssembler assembler;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public ProveedorResponse crear(ProveedorRequest request) {
        String codigo = AdminCodigos.normalizar(request.getCodigo());
        proveedores.findByCodigo(codigo).ifPresent(p -> {
            throw new AdminConflictException("Ya existe el proveedor " + codigo + " (id " + p.getId() + ")");
        });
        Proveedor saved = proveedores.save(Proveedor.builder()
                .codigo(codigo)
                .nombre(request.getNombre().trim())
                .baseUrl(request.getBaseUrl().trim())
                .connectTimeoutMs(request.getConnectTimeoutMs() != null ? request.getConnectTimeoutMs() : DEFAULT_CONNECT_TIMEOUT_MS)
                .readTimeoutMs(request.getReadTimeoutMs() != null ? request.getReadTimeoutMs() : DEFAULT_READ_TIMEOUT_MS)
                .activo(request.getActivo() == null || request.getActivo())
                .build());
        log.info("[SMS-ADMIN] Proveedor creado: {} (id {})", codigo, saved.getId());
        routingCache.invalidarAlConfirmar();
        return assembler.toResponse(saved);
    }
}
