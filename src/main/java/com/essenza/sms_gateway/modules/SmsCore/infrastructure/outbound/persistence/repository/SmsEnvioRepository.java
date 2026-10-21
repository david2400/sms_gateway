package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsEnvioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SmsEnvioRepository extends JpaRepository<SmsEnvioEntity, Long>, JpaSpecificationExecutor<SmsEnvioEntity> {

    @EntityGraph(attributePaths = {"proveedorFinal", "cuentaFinal"})
    Optional<SmsEnvioEntity> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"proveedorFinal", "cuentaFinal"})
    Optional<SmsEnvioEntity> findWithFinalByUuid(UUID uuid);

    Optional<SmsEnvioEntity> findByUuid(UUID uuid);
}
