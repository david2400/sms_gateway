package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantillaSmsRepository extends JpaRepository<PlantillaSmsEntity, Long> {

    @EntityGraph(attributePaths = {"tipoMensaje", "variables"})
    Optional<PlantillaSmsEntity> findWithVariablesByIdSmsPlantilla(Long id);

    @EntityGraph(attributePaths = {"tipoMensaje", "variables"})
    Optional<PlantillaSmsEntity> findByCodigo(String codigo);

    /** La que usa el motor: solo activas. */
    @EntityGraph(attributePaths = {"tipoMensaje", "variables"})
    Optional<PlantillaSmsEntity> findByCodigoAndEliminadoFalse(String codigo);

    @EntityGraph(attributePaths = {"tipoMensaje", "variables"})
    List<PlantillaSmsEntity> findAllByOrderByCodigoAsc();
}
