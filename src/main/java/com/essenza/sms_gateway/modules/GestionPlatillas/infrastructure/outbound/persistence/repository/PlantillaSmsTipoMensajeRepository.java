package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsTipoMensajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlantillaSmsTipoMensajeRepository extends JpaRepository<PlantillaSmsTipoMensajeEntity, Long> {
    Optional<PlantillaSmsTipoMensajeEntity> findByCodigo(String codigo);
}
