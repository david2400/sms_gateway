package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.CanalCuentaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CanalCuentaRepository extends JpaRepository<CanalCuentaEntity, Long> {

    @EntityGraph(attributePaths = "canalProveedor")
    Optional<CanalCuentaEntity> findByCodigo(String codigo);

    @EntityGraph(attributePaths = "canalProveedor")
    Optional<CanalCuentaEntity> findByCodigoAndCanalProveedor_Codigo(String codigo, String proveedorCodigo);

    @EntityGraph(attributePaths = "canalProveedor")
    Optional<CanalCuentaEntity> findFirstByCanalProveedor_CodigoAndEliminadoFalseOrderByIdCanalCuentaAsc(String proveedorCodigo);
}
