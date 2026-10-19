package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.CuentaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.AdminPersistenceMapper;
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
    private final AdminPersistenceMapper mapper;

    @Override
    public List<Cuenta> findAll(Long proveedorId) {
        List<AdminCuentaEntity> list = proveedorId == null
                ? repository.findAllByOrderByCodigoAsc()
                : repository.findByProveedor_IdOrderByCodigoAsc(proveedorId);
        return list.stream().map(mapper::toCuenta).toList();
    }

    @Override
    public Optional<Cuenta> findById(Long id) {
        return repository.findWithProveedorById(id).map(mapper::toCuenta);
    }

    @Override
    public Optional<Cuenta> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toCuenta);
    }

    @Override
    public Cuenta save(Cuenta c) {
        AdminCuentaEntity e = c.getId() != null
                ? repository.findById(c.getId()).orElseThrow()
                : new AdminCuentaEntity();
        e.setProveedor(proveedorRepository.getReferenceById(c.getProveedorId()));
        e.setCodigo(c.getCodigo());
        e.setUsuario(c.getUsuario());
        e.setSecretRef(c.getSecretRef());
        e.setRemitente(c.getRemitente());
        e.setParametrosJson(mapper.toJson(c.getParametros()));
        e.setActivo(c.isActivo());
        Long id = repository.saveAndFlush(e).getId();
        return repository.findWithProveedorById(id).map(mapper::toCuenta).orElseThrow();
    }
}
