package com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.Plantilla;

import java.util.List;
import java.util.Optional;

/** Persistencia de {@code SmsPlantilla} + {@code SmsPlantillaVariable}. */
public interface PlantillaAdminPort {
    List<Plantilla> findAll();
    Optional<Plantilla> findById(Long id);
    /** Código normalizado (mayúsculas). */
    Optional<Plantilla> findByCodigo(String codigo);
    /** Guarda la plantilla y REEMPLAZA sus variables. */
    Plantilla save(Plantilla plantilla);
}
