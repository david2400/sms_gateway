package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.AdminPersistenceMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminProveedorRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminRutaRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adapter JPA de {@code sms_ruta}. */
@Component
@RequiredArgsConstructor
public class RutaAdminJpaAdapter implements RutaAdminPort {

    private final AdminRutaRepository rutaRepository;
    private final AdminProveedorRepository proveedorRepository;
    private final AdminPersistenceMapper mapper;

    @Override
    public List<Ruta> findAll(Long proveedorId, boolean soloCentral, String tipoMensaje) {
        return rutaRepository.findAllByOrderByIdAsc().stream()
                .map(mapper::toRuta)
                .filter(r -> !soloCentral || r.isCentral())
                .filter(r -> proveedorId == null || Objects.equals(proveedorId, r.getProveedorId()))
                .filter(r -> tipoMensaje == null || tipoMensaje.equals(r.getTipoMensaje()))
                .toList();
    }

    @Override
    public Optional<Ruta> findById(Long id) {
        return rutaRepository.findWithProveedorById(id).map(mapper::toRuta);
    }

    @Override
    public Optional<Ruta> findByProveedorAndTipo(Long proveedorId, String tipoMensaje) {
        return (proveedorId == null
                ? rutaRepository.findFirstByProveedorIsNullAndTipoMensaje(tipoMensaje)
                : rutaRepository.findFirstByProveedor_IdAndTipoMensaje(proveedorId, tipoMensaje))
                .map(mapper::toRuta);
    }

    @Override
    public Ruta save(Ruta r) {
        AdminRutaEntity e = r.getId() != null ? rutaRepository.findById(r.getId()).orElseThrow() : new AdminRutaEntity();
        e.setProveedor(r.getProveedorId() != null ? proveedorRepository.getReferenceById(r.getProveedorId()) : null);
        e.setTipoMensaje(r.getTipoMensaje());
        e.setActivo(r.isActivo());
        Long id = rutaRepository.saveAndFlush(e).getId();
        return rutaRepository.findWithProveedorById(id).map(mapper::toRuta).orElseThrow();
    }

    @Override
    public void delete(Long rutaId) {
        rutaRepository.deleteById(rutaId);
        rutaRepository.flush();
    }
}
