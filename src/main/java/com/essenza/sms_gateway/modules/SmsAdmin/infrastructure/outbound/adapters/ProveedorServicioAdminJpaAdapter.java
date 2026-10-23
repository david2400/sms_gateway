package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorServicioAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.TipoMensajeMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminProveedorServicioEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminProveedorRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminProveedorServicioRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminRutaPasoRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminTipoMensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Adapter JPA de {@code SmsProveedorServicio}. */
@Component
@RequiredArgsConstructor
public class ProveedorServicioAdminJpaAdapter implements ProveedorServicioAdminPort {

    private final AdminProveedorServicioRepository repository;
    private final AdminProveedorRepository proveedores;
    private final AdminTipoMensajeRepository tipos;
    private final AdminRutaPasoRepository pasos;
    private final TipoMensajeMapper tipoMapper;

    @Override
    public List<TipoMensaje> tiposDe(Long proveedorId) {
        return repository.findByProveedor_IdSmsProveedorAndActivoTrueOrderByTipoMensaje_CodigoAsc(proveedorId).stream()
                .map(s -> tipoMapper.toModel(s.getTipoMensaje()))
                .toList();
    }

    @Override
    public boolean soporta(Long proveedorId, Long tipoMensajeId) {
        return proveedorId != null && tipoMensajeId != null
                && repository.existsByProveedor_IdSmsProveedorAndTipoMensaje_IdSmsTipoMensajeAndActivoTrue(proveedorId, tipoMensajeId);
    }

    @Override
    public void reemplazar(Long proveedorId, Set<Long> tipoMensajeIds) {
        repository.deleteByProveedorId(proveedorId);
        List<AdminProveedorServicioEntity> nuevos = tipoMensajeIds.stream().map(tipoId -> {
            AdminProveedorServicioEntity e = new AdminProveedorServicioEntity();
            e.setProveedor(proveedores.getReferenceById(proveedorId));
            e.setTipoMensaje(tipos.getReferenceById(tipoId));
            e.setActivo(Boolean.TRUE);
            e.setFechaCreacion(LocalDateTime.now());
            return e;
        }).toList();
        repository.saveAllAndFlush(nuevos);
    }

    @Override
    public List<String> rutasQueUsan(Long proveedorId, Long tipoMensajeId) {
        return pasos.findRutasByProveedorAndTipo(proveedorId, tipoMensajeId).stream()
                .map(r -> (r.getProveedor() == null ? "CENTRAL" : r.getProveedor().getCodigo())
                        + "/" + r.getTipoMensaje().getCodigo() + " (id " + r.getIdSmsRuta() + ")")
                .toList();
    }
}
