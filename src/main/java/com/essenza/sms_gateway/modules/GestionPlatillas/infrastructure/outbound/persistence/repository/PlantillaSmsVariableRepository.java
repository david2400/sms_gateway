package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsVariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface PlantillaSmsVariableRepository extends JpaRepository<PlantillaSmsVariableEntity, Long> {

    /**
     * Borra físicamente las variables ya eliminadas (soft delete) de la plantilla que tienen alguno de los nombres
     * indicados. Siguen ocupando el UNIQUE (plantilla_id, nombre) aunque JPA no las vea (@SoftDelete), y sin esto
     * volver a declarar una variable quitada antes da "código o variable duplicada".
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM SmsPlantillaVariable WHERE plantilla_id = :plantillaId AND eliminado = 1 "
            + "AND nombre IN (:nombres)", nativeQuery = true)
    int purgarEliminadas(@Param("plantillaId") Long plantillaId, @Param("nombres") Collection<String> nombres);
}
