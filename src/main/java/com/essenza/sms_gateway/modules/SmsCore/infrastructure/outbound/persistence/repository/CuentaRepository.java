package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.CuentaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<CuentaEntity, Long> {

    @EntityGraph(attributePaths = "proveedor")
    Optional<CuentaEntity> findByCodigo(String codigo);

    @EntityGraph(attributePaths = "proveedor")
    Optional<CuentaEntity> findByCodigoAndProveedor_Codigo(String codigo, String proveedorCodigo);

    @EntityGraph(attributePaths = "proveedor")
    Optional<CuentaEntity> findFirstByProveedor_CodigoAndEliminadoFalseOrderByIdCuentaAsc(String proveedorCodigo);
}
