package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.ProveedorServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProveedorServicioRepository extends JpaRepository<ProveedorServicioEntity, Long> {

    /** Servicios activos de proveedores activos (catálogo completo, es pequeño). */
    @Query("select ps from ProveedorServicioEntity ps join fetch ps.proveedor p join fetch ps.tipoMensaje t "
            + "where ps.eliminado = false and p.eliminado = false")
    List<ProveedorServicioEntity> findAllActive();
}
