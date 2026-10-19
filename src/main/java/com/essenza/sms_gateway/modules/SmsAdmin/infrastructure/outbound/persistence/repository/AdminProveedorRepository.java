package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositorio de administración de sms_proveedor. */
public interface AdminProveedorRepository extends JpaRepository<AdminProveedorEntity, Long> {
    Optional<AdminProveedorEntity> findByCodigo(String codigo);
    List<AdminProveedorEntity> findAllByOrderByCodigoAsc();
}
