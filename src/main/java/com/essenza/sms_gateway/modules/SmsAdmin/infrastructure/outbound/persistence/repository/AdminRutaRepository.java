package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRutaRepository extends JpaRepository<AdminRutaEntity, Long> {

    @EntityGraph(attributePaths = "proveedor")
    List<AdminRutaEntity> findAllByOrderByIdSmsRutaAsc();

    @EntityGraph(attributePaths = "proveedor")
    Optional<AdminRutaEntity> findWithProveedorByIdSmsRuta(Long id);

    @EntityGraph(attributePaths = "proveedor")
    Optional<AdminRutaEntity> findFirstByProveedorIsNullAndTipoMensaje(String tipoMensaje);

    @EntityGraph(attributePaths = "proveedor")
    Optional<AdminRutaEntity> findFirstByProveedor_IdSmsProveedorAndTipoMensaje(Long proveedorId, String tipoMensaje);
}
