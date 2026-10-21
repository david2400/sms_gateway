package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.TipoMensajeAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.TipoMensajeMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminTipoMensajeEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminTipoMensajeRepository;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.CanalMensaje;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adapter JPA de {@code sms_tipo_mensaje}. */
@Component
@RequiredArgsConstructor
public class TipoMensajeAdminJpaAdapter implements TipoMensajeAdminPort {

    private final AdminTipoMensajeRepository repository;
    private final TipoMensajeMapper mapper;

    @Override
    public List<TipoMensaje> findAll(CanalMensaje canal, Boolean soloOtp, Boolean soloMasivo) {
        return repository.findAllByOrderByCodigoAsc().stream()
                .map(mapper::toModel)
                .filter(t -> canal == null || t.getCanal() == canal)
                .filter(t -> !Boolean.TRUE.equals(soloOtp) || Boolean.TRUE.equals(t.getOtp()))
                .filter(t -> !Boolean.TRUE.equals(soloMasivo) || Boolean.TRUE.equals(t.getMasivo()))
                .toList();
    }

    @Override
    public Optional<TipoMensaje> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Optional<TipoMensaje> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toModel);
    }

    @Override
    public TipoMensaje save(TipoMensaje tipo) {
        AdminTipoMensajeEntity e = tipo.getId() != null
                ? repository.findById(tipo.getId()).orElseThrow()
                : new AdminTipoMensajeEntity();
        mapper.updateEntity(tipo, e);
        return mapper.toModel(repository.saveAndFlush(e));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
        repository.flush();
    }
}
