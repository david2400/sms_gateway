package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsEnvioIntentoEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SmsEnvioIntentoRepository extends JpaRepository<SmsEnvioIntentoEntity, Long> {

    @EntityGraph(attributePaths = {"canalProveedor", "canalCuenta"})
    List<SmsEnvioIntentoEntity> findByEnvio_UuidOrderByIdSmsEnvioIntentoAsc(UUID envioId);
}
