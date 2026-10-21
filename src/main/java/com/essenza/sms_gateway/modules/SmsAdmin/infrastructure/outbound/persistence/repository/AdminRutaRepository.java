package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRutaRepository extends JpaRepository<AdminRutaEntity, Long> {

    @EntityGraph(attributePaths = {"proveedor", "tipoMensaje"})
    List<AdminRutaEntity> findAllByOrderByIdSmsRutaAsc();

    @EntityGraph(attributePaths = {"proveedor", "tipoMensaje"})
    Optional<AdminRutaEntity> findWithProveedorByIdSmsRuta(Long id);

    @EntityGraph(attributePaths = {"proveedor", "tipoMensaje"})
    Optional<AdminRutaEntity> findFirstByProveedorIsNullAndTipoMensaje_IdSmsTipoMensaje(Long tipoMensajeId);

    @EntityGraph(attributePaths = {"proveedor", "tipoMensaje"})
    Optional<AdminRutaEntity> findFirstByProveedor_IdSmsProveedorAndTipoMensaje_IdSmsTipoMensaje(Long proveedorId, Long tipoMensajeId);
}
