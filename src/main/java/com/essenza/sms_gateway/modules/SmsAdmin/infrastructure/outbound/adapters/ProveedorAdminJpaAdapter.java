package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.ProveedorEntityMapper;
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
    private final ProveedorEntityMapper mapper;

    @Override
    public List<Proveedor> findAll() {
        return repository.findAllByOrderByCodigoAsc().stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Proveedor> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Optional<Proveedor> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toModel);
    }

    @Override
    public Proveedor save(Proveedor p) {
        AdminProveedorEntity e = p.getId() != null
                ? repository.findById(p.getId()).orElseThrow()
                : new AdminProveedorEntity();
        mapper.updateEntity(p, e);
        return mapper.toModel(repository.saveAndFlush(e));
    }
}
