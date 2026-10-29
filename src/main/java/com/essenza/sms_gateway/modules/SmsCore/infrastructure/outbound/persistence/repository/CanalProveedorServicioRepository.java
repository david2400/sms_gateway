package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.CanalProveedorServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CanalProveedorServicioRepository extends JpaRepository<CanalProveedorServicioEntity, Long> {

    /** Servicios activos de proveedores activos (catálogo completo, es pequeño). */
    @Query("select ps from CanalProveedorServicioEntity ps join fetch ps.canalProveedor p join fetch ps.tipoMensaje t "
            + "where ps.eliminado = false and p.eliminado = false")
    List<CanalProveedorServicioEntity> findAllActive();
}
