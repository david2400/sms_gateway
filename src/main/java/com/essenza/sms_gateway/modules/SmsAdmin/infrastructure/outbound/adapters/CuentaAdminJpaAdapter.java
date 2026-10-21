package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.CuentaMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminCuentaRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminProveedorRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminCuentaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CuentaAdminJpaAdapter implements CuentaAdminPort {

    private final AdminCuentaRepository repository;
    private final AdminProveedorRepository proveedorRepository;
    private final CuentaMapper mapper;

    @Override
    public List<Cuenta> findAll(Long proveedorId) {
        List<AdminCuentaEntity> list = proveedorId == null
                ? repository.findAllByOrderByCodigoAsc()
                : repository.findByProveedor_IdSmsProveedorOrderByCodigoAsc(proveedorId);
        return list.stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Cuenta> findById(Long id) {
        return repository.findWithProveedorByIdSmsCuenta(id).map(mapper::toModel);
    }

    @Override
    public Optional<Cuenta> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toModel);
    }

    @Override
    public Cuenta save(Cuenta c) {
        AdminCuentaEntity e = c.getId() != null
                ? repository.findById(c.getId()).orElseThrow()
                : new AdminCuentaEntity();
        mapper.updateEntity(c, e);
        e.setProveedor(proveedorRepository.getReferenceById(c.getProveedorId()));
        Long id = repository.saveAndFlush(e).getIdSmsCuenta();
        return repository.findWithProveedorByIdSmsCuenta(id).map(mapper::toModel).orElseThrow();
    }
}
