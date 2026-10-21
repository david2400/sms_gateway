package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaPasoEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminRutaPasoRepository extends JpaRepository<AdminRutaPasoEntity, Long> {

    @EntityGraph(attributePaths = {"cuenta", "cuenta.proveedor"})
    List<AdminRutaPasoEntity> findByRuta_IdSmsRutaOrderByOrdenAsc(Long rutaId);

    /** Borrado inmediato (antes de insertar los nuevos pasos, por el UNIQUE ruta_id+orden). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AdminRutaPaso p where p.ruta.idSmsRuta = :rutaId")
    int deleteByRutaId(@Param("rutaId") Long rutaId);

    @Query("select distinct p.ruta.idSmsRuta from AdminRutaPaso p where p.cuenta.idSmsCuenta = :cuentaId order by p.ruta.idSmsRuta")
    List<Long> findRutaIdsByCuentaId(@Param("cuentaId") Long cuentaId);
}
