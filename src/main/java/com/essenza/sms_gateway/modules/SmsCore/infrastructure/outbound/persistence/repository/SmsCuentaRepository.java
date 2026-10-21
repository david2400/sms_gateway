package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsCuentaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmsCuentaRepository extends JpaRepository<SmsCuentaEntity, Long> {

    @EntityGraph(attributePaths = "proveedor")
    Optional<SmsCuentaEntity> findByCodigoAndProveedor_Codigo(String codigo, String proveedorCodigo);

    @EntityGraph(attributePaths = "proveedor")
    Optional<SmsCuentaEntity> findFirstByProveedor_CodigoAndActivoTrueOrderByIdSmsCuentaAsc(String proveedorCodigo);
}
