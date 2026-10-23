package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminProveedorServicioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminProveedorServicioRepository extends JpaRepository<AdminProveedorServicioEntity, Long> {

    @EntityGraph(attributePaths = {"tipoMensaje"})
    List<AdminProveedorServicioEntity> findByProveedor_IdSmsProveedorAndEliminadoFalseOrderByTipoMensaje_CodigoAsc(Long proveedorId);

    boolean existsByProveedor_IdSmsProveedorAndTipoMensaje_IdSmsTipoMensajeAndEliminadoFalse(Long proveedorId, Long tipoMensajeId);

    /** Borrado físico (tabla de relación) antes de insertar la nueva lista. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AdminProveedorServicio s where s.proveedor.idSmsProveedor = :proveedorId")
    int deleteByProveedorId(@Param("proveedorId") Long proveedorId);
}
