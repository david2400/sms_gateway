package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminCuentaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminCuentaRepository extends JpaRepository<AdminCuentaEntity, Long> {

    @EntityGraph(attributePaths = "proveedor")
    List<AdminCuentaEntity> findAllByOrderByCodigoAsc();

    @EntityGraph(attributePaths = "proveedor")
    List<AdminCuentaEntity> findByProveedor_IdOrderByCodigoAsc(Long proveedorId);

    @EntityGraph(attributePaths = "proveedor")
    Optional<AdminCuentaEntity> findWithProveedorById(Long id);

    @EntityGraph(attributePaths = "proveedor")
    Optional<AdminCuentaEntity> findByCodigo(String codigo);

}
