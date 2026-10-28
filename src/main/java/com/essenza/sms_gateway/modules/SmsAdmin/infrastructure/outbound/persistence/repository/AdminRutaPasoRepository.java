package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.SmsRutaPasoEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminRutaPasoRepository extends JpaRepository<SmsRutaPasoEntity, Long> {

    @EntityGraph(attributePaths = {"cuenta", "cuenta.proveedor"})
    List<SmsRutaPasoEntity> findByRuta_IdSmsRutaOrderByOrdenAsc(Long rutaId);

    /** Borrado inmediato (antes de insertar los nuevos pasos, por el UNIQUE ruta_id+orden). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AdminRutaPaso p where p.ruta.idSmsRuta = :rutaId")
    int deleteByRutaId(@Param("rutaId") Long rutaId);

    @Query("select distinct p.ruta.idSmsRuta from AdminRutaPaso p where p.cuenta.idSmsCuenta = :cuentaId order by p.ruta.idSmsRuta")
    List<Long> findRutaIdsByCuentaId(@Param("cuentaId") Long cuentaId);

    /** Rutas del tipo que tienen pasos con cuentas del proveedor (para no quitar un servicio en uso). */
    @Query("select distinct r from AdminRutaPaso p join p.ruta r left join fetch r.proveedor join fetch r.tipoMensaje "
            + "where p.cuenta.proveedor.idSmsProveedor = :proveedorId and r.tipoMensaje.idSmsTipoMensaje = :tipoId")
    List<AdminRutaEntity> findRutasByProveedorAndTipo(@Param("proveedorId") Long proveedorId, @Param("tipoId") Long tipoId);
}
