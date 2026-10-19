package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.AdminPersistenceMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminProveedorRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminProveedorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProveedorAdminJpaAdapter implements ProveedorAdminPort {

    private final AdminProveedorRepository repository;
    private final AdminPersistenceMapper mapper;

    @Override
    public List<Proveedor> findAll() {
        return repository.findAllByOrderByCodigoAsc().stream().map(mapper::toProveedor).toList();
    }

    @Override
    public Optional<Proveedor> findById(Long id) {
        return repository.findById(id).map(mapper::toProveedor);
    }

    @Override
    public Optional<Proveedor> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toProveedor);
    }

    @Override
    public Proveedor save(Proveedor p) {
        AdminProveedorEntity e = p.getId() != null
                ? repository.findById(p.getId()).orElseThrow()
                : new AdminProveedorEntity();
        e.setCodigo(p.getCodigo());
        e.setNombre(p.getNombre());
        e.setBaseUrl(p.getBaseUrl());
        e.setConnectTimeoutMs(p.getConnectTimeoutMs());
        e.setReadTimeoutMs(p.getReadTimeoutMs());
        e.setActivo(p.isActivo());
        return mapper.toProveedor(repository.saveAndFlush(e));
    }
}
