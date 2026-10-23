package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.Plantilla;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.PlantillaVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaAdminPort;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.mappers.PlantillaMapper;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsEntity;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsVariableEntity;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository.PlantillaSmsRepository;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository.PlantillaSmsTipoMensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adapter JPA de la administración de plantillas (variables en cascada, reemplazo completo al guardar). */
@Component
@RequiredArgsConstructor
public class PlantillaAdminJpaAdapter implements PlantillaAdminPort {

    private final PlantillaSmsRepository repository;
    private final PlantillaSmsTipoMensajeRepository tipos;
    private final PlantillaMapper mapper;

    @Override
    public List<Plantilla> findAll() {
        return repository.findAllByOrderByCodigoAsc().stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Plantilla> findById(Long id) {
        return repository.findWithVariablesByIdSmsPlantilla(id).map(mapper::toModel);
    }

    @Override
    public Optional<Plantilla> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toModel);
    }

    @Override
    public Plantilla save(Plantilla p) {
        PlantillaSmsEntity e = p.getId() != null
                ? repository.findWithVariablesByIdSmsPlantilla(p.getId()).orElseThrow()
                : new PlantillaSmsEntity();
        mapper.updateEntity(p, e);
        e.setTipoMensaje(p.getTipoMensajeId() == null ? null : tipos.getReferenceById(p.getTipoMensajeId()));

        // Reemplazo completo: orphanRemoval borra las anteriores. Flush entre borrar e insertar
        // por el UNIQUE (plantilla_id, nombre).
        e.getVariables().clear();
        if (e.getIdSmsPlantilla() != null) {
            repository.saveAndFlush(e);
        }
        for (PlantillaVariable v : p.getVariables()) {
            PlantillaSmsVariableEntity ve = mapper.toEntity(v);
            ve.setTipoDato(v.getTipoDato().name());
            ve.setPlantilla(e);
            e.getVariables().add(ve);
        }
        return mapper.toModel(repository.saveAndFlush(e));
    }
}
