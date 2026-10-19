package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmsProveedorRepository extends JpaRepository<SmsProveedorEntity, Long> {

    Optional<SmsProveedorEntity> findByCodigo(String codigo);
}
