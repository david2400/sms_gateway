package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsProveedorServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SmsProveedorServicioRepository extends JpaRepository<SmsProveedorServicioEntity, Long> {

    /** Servicios activos de proveedores activos (catálogo completo, es pequeño). */
    @Query("select ps from SmsProveedorServicioEntity ps join fetch ps.proveedor p join fetch ps.tipoMensaje t "
            + "where ps.activo = true and p.activo = true")
    List<SmsProveedorServicioEntity> findAllActive();
}
