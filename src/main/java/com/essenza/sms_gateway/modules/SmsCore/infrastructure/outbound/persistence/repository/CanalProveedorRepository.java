package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.CanalProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CanalProveedorRepository extends JpaRepository<CanalProveedorEntity, Long> {

    Optional<CanalProveedorEntity> findByCodigo(String codigo);
}
