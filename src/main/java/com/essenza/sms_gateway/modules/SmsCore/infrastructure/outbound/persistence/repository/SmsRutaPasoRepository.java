package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsRutaPasoEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsRutaPasoRepository extends JpaRepository<SmsRutaPasoEntity, Long> {

    @EntityGraph(attributePaths = {"cuenta", "cuenta.proveedor"})
    List<SmsRutaPasoEntity> findByRuta_IdSmsRutaAndActivoTrueOrderByOrdenAsc(Long rutaId);
}
