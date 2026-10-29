package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.RutaPasoEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RutaPasoRepository extends JpaRepository<RutaPasoEntity, Long> {

    @EntityGraph(attributePaths = {"canalCuenta", "canalCuenta.canalProveedor"})
    List<RutaPasoEntity> findByRuta_IdRutaAndEliminadoFalseOrderByOrdenAsc(Long rutaId);
}
