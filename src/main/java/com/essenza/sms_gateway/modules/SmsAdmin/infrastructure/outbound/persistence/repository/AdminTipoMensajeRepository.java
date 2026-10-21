package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminTipoMensajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositorio de administración de sms_tipo_mensaje. */
public interface AdminTipoMensajeRepository extends JpaRepository<AdminTipoMensajeEntity, Long> {
    Optional<AdminTipoMensajeEntity> findByCodigo(String codigo);
    List<AdminTipoMensajeEntity> findAllByOrderByCodigoAsc();
}
